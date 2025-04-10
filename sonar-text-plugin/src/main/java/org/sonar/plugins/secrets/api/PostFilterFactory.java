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

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.plugins.secrets.configuration.model.matching.filter.AbstractPostModule;
import org.sonar.plugins.secrets.configuration.model.matching.filter.DecodedBase64Module;
import org.sonar.plugins.secrets.configuration.model.matching.filter.HeuristicsFilter;
import org.sonar.plugins.secrets.configuration.model.matching.filter.StatisticalFilter;

/**
 * Factory class to create a predicate based on the post module configuration.
 */
public final class PostFilterFactory {
  private static final Logger LOG = LoggerFactory.getLogger(PostFilterFactory.class);

  private PostFilterFactory() {
  }

  /**
   * Entry method of this class to create a predicate based on the post module configuration.
   * @param post deserialized post module configuration
   * @return a predicate to filter out potential secrets based on the post module configuration
   */
  public static Predicate<String> createPredicate(@Nullable AbstractPostModule post) {
    Predicate<String> postFilter = s -> true;
    if (post != null) {
      if (post.getDecodedBase64Module() != null) {
        postFilter = postFilter.and(input -> matchBase64Decoded(post.getDecodedBase64Module(), input));
      }
      if (post.getStatisticalFilter() != null) {
        postFilter = postFilter.and(filterForStatisticalFilter(post.getStatisticalFilter()));
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

  static Predicate<String> filterForStatisticalFilter(StatisticalFilter statisticalFilter) {
    return (String candidateSecret) -> !EntropyChecker.hasLowEntropy(candidateSecret, statisticalFilter.getThreshold());
  }

  static Predicate<String> filterForHeuristicsFilter(HeuristicsFilter heuristicFilter) {
    return candidateSecret -> !Heuristics.matchesHeuristics(candidateSecret, heuristicFilter.getHeuristics());
  }

  static boolean matchBase64Decoded(DecodedBase64Module decodedBase64Module, String candidateSecret) {
    byte[] decodedBytes;
    try {
      decodedBytes = Base64.getDecoder().decode(candidateSecret);
    } catch (IllegalArgumentException iae) {
      LOG.debug("Base64 decoding failed for input: {}", candidateSecret);
      // If decoding failed, then this is not what we were looking for
      return false;
    }
    var decoded = new String(decodedBytes, StandardCharsets.UTF_8);
    return decodedBase64Module.matchEach().stream().allMatch(decoded::contains);
  }
}
