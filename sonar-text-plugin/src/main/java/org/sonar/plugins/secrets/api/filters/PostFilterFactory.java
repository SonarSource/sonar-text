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
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.plugins.secrets.api.EntropyChecker;
import org.sonar.plugins.secrets.api.Heuristics;
import org.sonar.plugins.secrets.configuration.model.matching.filter.AbstractPostModule;
import org.sonar.plugins.secrets.configuration.model.matching.filter.DecodedBase64Module;
import org.sonar.plugins.secrets.configuration.model.matching.filter.HeuristicsFilter;
import org.sonar.plugins.secrets.configuration.model.matching.filter.StatisticalFilter;

/**
 * Factory class to create a post-filter based on the post module configuration.
 *
 * <p>The factory composes one {@link PostFilterHandler} per supported filter type. Adding a new filter type means
 * registering a new handler in {@link #HANDLERS}; adding a new {@link SkippedFilter} means wiring it into the handler(s) that
 * own the affected filter. {@link #createFilter(AbstractPostModule, Set)} itself does not need to change.
 */
public final class PostFilterFactory {
  private static final Logger LOG = LoggerFactory.getLogger(PostFilterFactory.class);

  private static final List<PostFilterHandler> HANDLERS = List.of(
    new DecodedBase64Handler(),
    new StatisticalFilterHandler(),
    new PatternNotHandler(),
    new HeuristicsHandler());

  private PostFilterFactory() {
  }

  /**
   * Creates a structured post-filter with no filters skipped.
   */
  public static PostFilter createFilter(@Nullable AbstractPostModule post) {
    return createFilter(post, Set.of());
  }

  /**
   * Creates a structured post-filter based on the post module configuration.
   *
   * <p>For each filter type present in {@code post}, a {@link PostFilter} is built by the corresponding handler. Each
   * handler is responsible for knowing whether any of the {@code skippedFilters} applies to it. The resulting
   * filters are combined into a single pipeline that reduces their outcomes with {@link FilterOutcome#combine}.
   *
   * @param post               deserialized post module configuration
   * @param skippedFilters filters to skip as requested by the caller
   */
  public static PostFilter createFilter(@Nullable AbstractPostModule post, Set<SkippedFilter> skippedFilters) {
    if (post == null) {
      return PostFilter.ACCEPT_ALL;
    }

    List<PostFilter> filters = HANDLERS.stream()
      .map(h -> h.build(post, skippedFilters))
      .filter(Objects::nonNull)
      .toList();

    if (filters.isEmpty()) {
      return PostFilter.ACCEPT_ALL;
    }

    return (String candidateSecret) -> {
      FilterOutcome aggregate = FilterOutcome.ACCEPTED;
      for (PostFilter filter : filters) {
        FilterOutcome outcome = filter.apply(candidateSecret);
        if (!outcome.passed()) {
          return FilterOutcome.REJECTED;
        }
        aggregate = aggregate.combine(outcome);
      }
      return aggregate;
    };
  }

  /**
   * Builds a {@link PostFilter} for a specific filter type (e.g., statistical, pattern-not). Returns {@code null} when
   * the filter type is not present in the given module.
   */
  interface PostFilterHandler {
    @Nullable
    PostFilter build(AbstractPostModule module, Set<SkippedFilter> skippedFilters);
  }

  private static final class DecodedBase64Handler implements PostFilterHandler {
    @Override
    public PostFilter build(AbstractPostModule module, Set<SkippedFilter> skippedFilters) {
      DecodedBase64Module decodedBase64Module = module.getDecodedBase64Module();
      if (decodedBase64Module == null) {
        return null;
      }
      return candidate -> matchBase64Decoded(decodedBase64Module, candidate) ? FilterOutcome.ACCEPTED : FilterOutcome.REJECTED;
    }
  }

  private static final class PatternNotHandler implements PostFilterHandler {
    @Override
    public PostFilter build(AbstractPostModule module, Set<SkippedFilter> skippedFilters) {
      List<String> patternNot = module.getPatternNot();
      if (patternNot.isEmpty()) {
        return null;
      }
      Pattern compiled = Pattern.compile(pipePatternNot(patternNot));
      return candidate -> compiled.matcher(candidate).find() ? FilterOutcome.REJECTED : FilterOutcome.ACCEPTED;
    }
  }

  private static final class HeuristicsHandler implements PostFilterHandler {
    @Override
    public PostFilter build(AbstractPostModule module, Set<SkippedFilter> skippedFilters) {
      HeuristicsFilter heuristicFilter = module.getHeuristicFilter();
      if (heuristicFilter == null) {
        return null;
      }
      return candidate -> Heuristics.matchesHeuristics(candidate, heuristicFilter.getHeuristics())
        ? FilterOutcome.REJECTED
        : FilterOutcome.ACCEPTED;
    }
  }

  private static final class StatisticalFilterHandler implements PostFilterHandler {
    @Override
    public PostFilter build(AbstractPostModule module, Set<SkippedFilter> skippedFilters) {
      StatisticalFilter statisticalFilter = module.getStatisticalFilter();
      if (statisticalFilter == null) {
        return null;
      }
      boolean entropyFilterDisabled = skippedFilters.contains(SkippedFilter.ENTROPY_FILTER);
      return candidate -> {
        boolean lowEntropy = EntropyChecker.hasLowEntropy(candidate, statisticalFilter.getThreshold());
        if (!lowEntropy) {
          return FilterOutcome.ACCEPTED;
        }
        return entropyFilterDisabled ? FilterOutcome.passedWithSkipped(SkippedFilter.ENTROPY_FILTER) : FilterOutcome.REJECTED;
      };
    }
  }

  static String pipePatternNot(List<String> patternNot) {
    var sb = new StringBuilder();
    for (var i = 0; i < patternNot.size(); i++) {
      sb.append("(?:");
      sb.append(patternNot.get(i));
      sb.append(")");
      if (i != patternNot.size() - 1) {
        sb.append("|");
      }
    }
    return sb.toString();
  }

  static boolean matchBase64Decoded(DecodedBase64Module decodedBase64Module, String candidateSecret) {
    byte[] decodedBytes;
    var stringToDecode = switch (decodedBase64Module.alphabet()) {
      case Y64 -> candidateSecret.replace('.', '+').replace('_', '/').replace('-', '=');
      case DEFAULT -> candidateSecret;
    };
    try {
      decodedBytes = Base64.getDecoder().decode(stringToDecode);
    } catch (IllegalArgumentException iae) {
      LOG.debug("Base64 decoding failed for input: {} (decoded with alphabet {})", stringToDecode, decodedBase64Module.alphabet());
      // If decoding failed, then this is not what we were looking for
      return false;
    }
    var decoded = new String(decodedBytes, StandardCharsets.UTF_8);
    var matchEachResult = decodedBase64Module.matchEach().isEmpty()
      || decodedBase64Module.matchEach().stream().allMatch(decoded::contains);
    var matchNotResult = decodedBase64Module.matchNot().stream().noneMatch(decoded::contains);
    return matchEachResult && matchNotResult;
  }
}
