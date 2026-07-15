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
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import org.sonar.plugins.secrets.api.EntropyChecker;
import org.sonar.plugins.secrets.api.Heuristics;
import org.sonar.plugins.secrets.configuration.model.matching.Matching;
import org.sonar.plugins.secrets.configuration.model.matching.filter.AbstractPostModule;
import org.sonar.plugins.secrets.configuration.model.matching.filter.DecodedBase64Module;

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
   * <p>The applicable handlers are combined via {@link #combineHandlers}, which short-circuits on the first rejection
   * and carries a detail string about why the candidate was rejected. This wrapper forwards that detail to
   * {@code rejectionLogger} on rejection — handlers themselves never reference the logger.
   *
   * @param post            deserialized post module configuration
   * @param skippedFilters  filters to skip as requested by the caller
   * @param rejectionLogger logger used to emit debug lines on rejection; pass {@link RejectionLogger#DISABLED} to opt out
   */
  public static PostFilter createFilter(@Nullable AbstractPostModule post, Set<SkippedFilter> skippedFilters, RejectionLogger rejectionLogger) {
    if (post == null) {
      return PostFilter.ACCEPT_ALL;
    }
    FilteringPostFilter combined = combineHandlers(post, skippedFilters);
    if (combined == null) {
      return PostFilter.ACCEPT_ALL;
    }
    return (String candidateSecret, RejectionLogContext context) -> {
      FilteringResult result = combined.apply(candidateSecret);
      if (!result.outcome().passed()) {
        rejectionLogger.log(context, result.details());
      }
      return result.outcome();
    };
  }

  /**
   * Combines every applicable handler for {@code module} into a single {@link FilteringPostFilter} that short-circuits
   * on the first rejection and aggregates {@link SkippedFilter} tags on acceptance. Used to run a nested post module
   * (e.g. the {@code post} block of {@code decodedBase64}) against an extracted value, without a
   * {@link RejectionLogContext} in scope. Returns {@code null} when the module declares no filters.
   */
  @Nullable
  static FilteringPostFilter combineHandlers(AbstractPostModule module, Set<SkippedFilter> skippedFilters) {
    List<FilteringPostFilter> filters = HANDLERS.stream()
      .map(h -> h.build(module, skippedFilters))
      .filter(Objects::nonNull)
      .toList();
    if (filters.isEmpty()) {
      return null;
    }
    return candidate -> {
      FilterOutcome aggregate = FilterOutcome.ACCEPTED;
      for (FilteringPostFilter filter : filters) {
        FilteringResult result = filter.apply(candidate);
        if (!result.outcome().passed()) {
          return result;
        }
        aggregate = aggregate.combine(result.outcome());
      }
      return new FilteringResult(aggregate, "");
    };
  }

  /**
   * Internal counterpart to {@link PostFilter} that also returns the safe-to-log description explaining a rejection.
   * Kept package-private because callers outside the factory only see the wrapped {@link PostFilter}.
   */
  @FunctionalInterface
  interface FilteringPostFilter {
    FilteringResult apply(String candidateSecret);
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
    FilteringPostFilter build(AbstractPostModule module, Set<SkippedFilter> skippedFilters);
  }

  private static final class DecodedBase64Handler implements PostFilterHandler {
    @Override
    public FilteringPostFilter build(AbstractPostModule module, Set<SkippedFilter> skippedFilters) {
      var decodedBase64Module = module.getDecodedBase64Module();
      if (decodedBase64Module == null) {
        return null;
      }
      Matching matching = decodedBase64Module.matching();
      Pattern extractionPattern = matching != null && matching.getPattern() != null
        ? Pattern.compile(matching.getPattern())
        : null;
      FilteringPostFilter nestedFilter = buildNestedFilter(decodedBase64Module, skippedFilters);
      return candidate -> evaluate(candidate, decodedBase64Module, extractionPattern, nestedFilter);
    }

    @Nullable
    private static FilteringPostFilter buildNestedFilter(DecodedBase64Module module, Set<SkippedFilter> skippedFilters) {
      var nestedPost = module.post();
      if (nestedPost == null) {
        return null;
      }
      // An empty nested post is rejected by the schema; null here (only from programmatic modules) is a no-op filter.
      return combineHandlers(nestedPost, skippedFilters);
    }

    private static FilteringResult evaluate(String candidate, DecodedBase64Module module,
      @Nullable Pattern extractionPattern, @Nullable FilteringPostFilter nestedFilter) {
      String decoded = decode(candidate, module.alphabet());
      if (decoded == null) {
        return FilteringResult.rejected("decodedBase64: invalid encoding");
      }
      FilteringResult literalResult = applyLiteralFilters(decoded, module);
      if (!literalResult.outcome().passed()) {
        return literalResult;
      }
      String extracted = extract(decoded, extractionPattern);
      if (extracted == null) {
        return FilteringResult.rejected("decodedBase64: matching not satisfied");
      }
      return applyNested(extracted, nestedFilter);
    }

    @Nullable
    private static String decode(String candidate, DecodedBase64Module.Alphabet alphabet) {
      var stringToDecode = switch (alphabet) {
        case Y64 -> candidate.replace('.', '+').replace('_', '/').replace('-', '=');
        case DEFAULT -> candidate;
      };
      try {
        return new String(Base64.getDecoder().decode(stringToDecode), StandardCharsets.UTF_8);
      } catch (IllegalArgumentException iae) {
        return null;
      }
    }

    private static FilteringResult applyLiteralFilters(String decoded, DecodedBase64Module module) {
      if (!module.matchEach().isEmpty() && !module.matchEach().stream().allMatch(decoded::contains)) {
        return FilteringResult.rejected("decodedBase64: matchEach not satisfied");
      }
      if (module.matchNot().stream().anyMatch(decoded::contains)) {
        return FilteringResult.rejected("decodedBase64: matchNot matched");
      }
      return FilteringResult.ACCEPTED;
    }

    /**
     * Applies the extraction pattern to the decoded value. Returns the whole decoded value when no pattern is
     * configured, the captured value (first group, else the whole match) on a hit, or {@code null} when a configured
     * pattern does not match.
     */
    @Nullable
    private static String extract(String decoded, @Nullable Pattern extractionPattern) {
      if (extractionPattern == null) {
        return decoded;
      }
      var matcher = extractionPattern.matcher(decoded);
      if (!matcher.find()) {
        return null;
      }
      // Convention (see PatternMatcher): the first capturing group is the extracted value; without one, the whole match.
      return matcher.groupCount() > 0 && matcher.group(1) != null ? matcher.group(1) : matcher.group();
    }

    private static FilteringResult applyNested(String extracted, @Nullable FilteringPostFilter nestedFilter) {
      if (nestedFilter == null) {
        return FilteringResult.ACCEPTED;
      }
      FilteringResult nestedResult = nestedFilter.apply(extracted);
      if (!nestedResult.outcome().passed()) {
        return FilteringResult.rejected("decodedBase64 -> " + nestedResult.details());
      }
      return nestedResult;
    }
  }

  private static final class PatternNotHandler implements PostFilterHandler {
    @Override
    public FilteringPostFilter build(AbstractPostModule module, Set<SkippedFilter> skippedFilters) {
      var patternNot = module.getPatternNot();
      if (patternNot.isEmpty()) {
        return null;
      }
      var patterns = patternNot.stream()
        .map(pattern -> Map.entry(pattern, Pattern.compile(pattern)))
        .toList();
      var knownFakeSecretFilterDisabled = skippedFilters.contains(SkippedFilter.KNOWN_FAKE_SECRET_FILTER);

      return candidate -> {
        for (var entry : patterns) {
          if (entry.getValue().matcher(candidate).find()) {
            if (knownFakeSecretFilterDisabled) {
              return new FilteringResult(FilterOutcome.passedWithSkipped(SkippedFilter.KNOWN_FAKE_SECRET_FILTER), "");
            }
            return FilteringResult.rejected("patternNot: " + entry.getKey());
          }
        }
        return FilteringResult.ACCEPTED;
      };
    }
  }

  private static final class HeuristicsHandler implements PostFilterHandler {
    @Override
    public FilteringPostFilter build(AbstractPostModule module, Set<SkippedFilter> skippedFilters) {
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
    public FilteringPostFilter build(AbstractPostModule module, Set<SkippedFilter> skippedFilters) {
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
