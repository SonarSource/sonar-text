/*
 * SonarQube Text Plugin
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.plugins.secrets.api.filters;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.function.Function;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import org.sonar.plugins.secrets.api.EntropyChecker;
import org.sonar.plugins.secrets.api.Heuristics;
import org.sonar.plugins.secrets.configuration.model.matching.filter.AbstractPostModule;

/**
 * Factory class to create a post-filter based on the post module configuration.
 *
 * <p>The factory composes one {@link PostFilterHandler} per supported filter type. Adding a new filter type means
 * registering a new handler in {@link #HANDLERS}; adding a new {@link SkippedFilter} means wiring it into the handler(s) that
 * own the affected filter. {@link #createFilter(AbstractPostModule, Set)} itself does not need to change.
 */
public final class PostFilterFactory {

  private static final List<PostFilterHandler> HANDLERS = List.of(
    new DecodedBase64Handler(),
    new StatisticalFilterHandler(),
    new PatternNotHandler(),
    new HeuristicsHandler());

  private PostFilterFactory() {
  }

  /**
   * Creates a structured post-filter with no filters skipped and rejection logging disabled.
   */
  public static PostFilter createFilter(@Nullable AbstractPostModule post) {
    return createFilter(post, Set.of(), RejectionLogger.DISABLED);
  }

  /**
   * Creates a structured post-filter with rejection logging disabled.
   */
  public static PostFilter createFilter(@Nullable AbstractPostModule post, Set<SkippedFilter> skippedFilters) {
    return createFilter(post, skippedFilters, RejectionLogger.DISABLED);
  }

  /**
   * Creates a structured post-filter based on the post module configuration.
   *
   * <p>For each filter type present in {@code post}, a {@link FilteringPostFilter} is built by the corresponding
   * handler. Each handler returns a {@link FilteringResult} carrying both the {@link FilterOutcome} and an optional
   * detail string about why a candidate was rejected. {@link #withRejectionLogging} then wraps each filter so the
   * {@code rejectionLogger} can emit a debug line — handlers themselves never reference the logger.
   *
   * @param post            deserialized post module configuration
   * @param skippedFilters  filters to skip as requested by the caller
   * @param rejectionLogger logger used to emit debug lines on rejection; pass {@link RejectionLogger#DISABLED} to opt out
   */
  public static PostFilter createFilter(@Nullable AbstractPostModule post, Set<SkippedFilter> skippedFilters, RejectionLogger rejectionLogger) {
    if (post == null) {
      return PostFilter.ACCEPT_ALL;
    }

    List<PostFilter> filters = HANDLERS.stream()
      .map(h -> {
        Function<String, FilteringResult> inner = h.build(post, skippedFilters);
        if (inner == null) {
          return null;
        }
        return withRejectionLogging(inner, rejectionLogger);
      })
      .filter(Objects::nonNull)
      .toList();

    if (filters.isEmpty()) {
      return PostFilter.ACCEPT_ALL;
    }

    return (String candidateSecret, RejectionLogContext context) -> {
      FilterOutcome aggregate = FilterOutcome.ACCEPTED;
      for (PostFilter filter : filters) {
        FilterOutcome outcome = filter.apply(candidateSecret, context);
        if (!outcome.passed()) {
          return FilterOutcome.REJECTED;
        }
        aggregate = aggregate.combine(outcome);
      }
      return aggregate;
    };
  }

  /**
   * Adapts a {@link FilteringPostFilter} (which carries a description string) into the public {@link PostFilter}
   * type (which only carries {@link FilterOutcome}). On rejection, the wrapper forwards the description to
   * {@code rejectionLogger}; on accept, the description is ignored.
   *
   * <p>Only the {@code inner} filter's own rejection triggers a log line — the outer pipeline short-circuits on the
   * first failing filter, so wrappers for later filters are never reached. This keeps each log line correctly
   * attributed to the specific filter that rejected the candidate.
   */
  private static PostFilter withRejectionLogging(Function<String, FilteringResult> inner, RejectionLogger rejectionLogger) {
    return (candidate, context) -> {
      FilteringResult result = inner.apply(candidate);
      if (!result.outcome().passed()) {
        rejectionLogger.log(context, result.details());
      }
      return result.outcome();
    };
  }

  /**
   * Outcome of a single {@link FilteringPostFilter} evaluation, together with a safe-to-log description of the
   * decision. On rejection the description identifies the filter and any specifics that help a specifier locate the
   * mis-tuned setting (e.g. {@code "patternNot: index=1"}, {@code "statistical (entropy): entropy=2.710, threshold=3.500"}).
   * On acceptance the description is empty and never read. It must not contain the candidate text or rule patterns.
   */
  record FilteringResult(FilterOutcome outcome, String details) {
    static final FilteringResult ACCEPTED = new FilteringResult(FilterOutcome.ACCEPTED, "");

    static FilteringResult rejected(String details) {
      return new FilteringResult(FilterOutcome.REJECTED, details);
    }
  }

  /**
   * Builds a {@link FilteringPostFilter} for a specific filter type (e.g., statistical, pattern-not). Returns
   * {@code null} when the filter type is not present in the given module.
   */
  interface PostFilterHandler {
    @Nullable
    Function<String, FilteringResult> build(AbstractPostModule module, Set<SkippedFilter> skippedFilters);
  }

  private static final class DecodedBase64Handler implements PostFilterHandler {
    @Override
    public Function<String, FilteringResult> build(AbstractPostModule module, Set<SkippedFilter> skippedFilters) {
      var decodedBase64Module = module.getDecodedBase64Module();
      if (decodedBase64Module == null) {
        return null;
      }
      return candidate -> {
        var stringToDecode = switch (decodedBase64Module.alphabet()) {
          case Y64 -> candidate.replace('.', '+').replace('_', '/').replace('-', '=');
          case DEFAULT -> candidate;
        };
        byte[] decodedBytes;
        try {
          decodedBytes = Base64.getDecoder().decode(stringToDecode);
        } catch (IllegalArgumentException iae) {
          return FilteringResult.rejected("decodedBase64: invalid encoding");
        }
        var decoded = new String(decodedBytes, StandardCharsets.UTF_8);
        if (!decodedBase64Module.matchEach().isEmpty()
          && !decodedBase64Module.matchEach().stream().allMatch(decoded::contains)) {
          return FilteringResult.rejected("decodedBase64: matchEach not satisfied");
        }
        if (decodedBase64Module.matchNot().stream().anyMatch(decoded::contains)) {
          return FilteringResult.rejected("decodedBase64: matchNot matched");
        }
        return FilteringResult.ACCEPTED;
      };
    }
  }

  private static final class PatternNotHandler implements PostFilterHandler {
    @Override
    public Function<String, FilteringResult> build(AbstractPostModule module, Set<SkippedFilter> skippedFilters) {
      var patternNot = module.getPatternNot();
      if (patternNot.isEmpty()) {
        return null;
      }
      var patterns = patternNot.stream()
        .map(pattern -> Map.entry(pattern, Pattern.compile(pattern)))
        .toList();

      return candidate -> {
        for (var entry : patterns) {
          if (entry.getValue().matcher(candidate).find()) {
            return FilteringResult.rejected("patternNot: " + entry.getKey());
          }
        }
        return FilteringResult.ACCEPTED;
      };
    }
  }

  private static final class HeuristicsHandler implements PostFilterHandler {
    @Override
    public Function<String, FilteringResult> build(AbstractPostModule module, Set<SkippedFilter> skippedFilters) {
      var heuristicFilter = module.getHeuristicFilter();
      if (heuristicFilter == null) {
        return null;
      }
      var heuristics = heuristicFilter.getHeuristics();
      // Check each named heuristic individually so we can report which one matched. Heuristic names ("path", "uri")
      // are a fixed, well-known vocabulary, not rule data.
      return candidate -> {
        for (var h : heuristics) {
          if (Heuristics.matchesHeuristic(candidate, h)) {
            return FilteringResult.rejected("heuristics: " + h);
          }
        }
        return FilteringResult.ACCEPTED;
      };
    }
  }

  private static final class StatisticalFilterHandler implements PostFilterHandler {
    @Override
    public Function<String, FilteringResult> build(AbstractPostModule module, Set<SkippedFilter> skippedFilters) {
      var statisticalFilter = module.getStatisticalFilter();
      if (statisticalFilter == null) {
        return null;
      }
      var entropyFilterDisabled = skippedFilters.contains(SkippedFilter.ENTROPY_FILTER);
      var threshold = statisticalFilter.getThreshold();
      return candidate -> {
        var entropy = EntropyChecker.calculateShannonEntropy(candidate);
        if (entropy >= threshold) {
          return FilteringResult.ACCEPTED;
        }
        if (entropyFilterDisabled) {
          return new FilteringResult(FilterOutcome.passedWithSkipped(SkippedFilter.ENTROPY_FILTER), "");
        }
        return FilteringResult.rejected(String.format(Locale.ROOT, "statistical (entropy): entropy=%.3f, threshold=%.3f", entropy, threshold));
      };
    }
  }

}
