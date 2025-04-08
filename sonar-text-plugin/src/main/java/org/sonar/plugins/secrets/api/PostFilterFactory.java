/*
 * SonarQube Text Plugin
 * Copyright (C) 2021-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.plugins.secrets.api;

import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import org.sonar.plugins.secrets.configuration.model.matching.Matching;
import org.sonar.plugins.secrets.configuration.model.matching.filter.AbstractPostModule;
import org.sonar.plugins.secrets.configuration.model.matching.filter.HeuristicsFilter;
import org.sonar.plugins.secrets.configuration.model.matching.filter.StatisticalFilter;

/**
 * Factory class to create a predicate based on the post module configuration.
 */
public final class PostFilterFactory {

  private PostFilterFactory() {
  }

  /**
   * Entry method of this class to create a predicate based on the post module configuration.
   * @param post deserialized post module configuration
   * @param matching deserialized matching configuration
   * @return a predicate to filter out potential secrets based on the post module configuration
   */
  public static Predicate<String> createPredicate(@Nullable AbstractPostModule post, @Nullable Matching matching) {
    Predicate<String> postFilter = s -> true;
    if (post != null) {
      if (post.getStatisticalFilter() != null) {
        postFilter = postFilter.and(filterForStatisticalFilter(post.getStatisticalFilter(), matching));
      }
      if (!post.getPatternNot().isEmpty()) {
        postFilter = postFilter.and(filterForPatternNot(post.getPatternNot()));
      }
      if (post.getHeuristicFilter() != null) {
        postFilter = postFilter.and(filterForHeuristicsFilter(post.getHeuristicFilter()));
      }
    }
    return postFilter;
  }

  static Predicate<String> filterForPatternNot(List<String> patternNot) {
    String pipedPatterns = pipePatternNot(patternNot);
    var compiledPatternNot = Pattern.compile(pipedPatterns);

    return (String candidateSecret) -> {
      var matcher = compiledPatternNot.matcher(candidateSecret);
      return !matcher.find();
    };
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

  static Predicate<String> filterForStatisticalFilter(StatisticalFilter statisticalFilter, @Nullable Matching matching) {
    return (String candidateSecret) -> {
      var entropyInputString = candidateSecret;
      if (statisticalFilter.getInputString() != null && matching != null) {
        entropyInputString = calculateEntropyInputBasedOnNamedGroup(statisticalFilter.getInputString(), candidateSecret, matching);
      }
      return !EntropyChecker.hasLowEntropy(entropyInputString, statisticalFilter.getThreshold());
    };
  }

  static String calculateEntropyInputBasedOnNamedGroup(String groupName, String candidateSecret, Matching matching) {
    var matcher = Pattern.compile(matching.getPattern()).matcher(candidateSecret);
    if (matcher.find()) {
      try {
        return matcher.group(groupName);
      } catch (IllegalArgumentException e) {
        // expected behavior to do nothing, as the fallback is candidate secret
      }
    }
    // matched group for the name not found, fallback to candidate secret
    return candidateSecret;
  }

  static Predicate<String> filterForHeuristicsFilter(HeuristicsFilter heuristicFilter) {
    return candidateSecret -> !Heuristics.matchesHeuristics(candidateSecret, heuristicFilter.getHeuristics());
  }
}
