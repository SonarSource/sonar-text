/*
 * SonarQube Text Plugin
 * Copyright (C) 2021-2023 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.plugins.secrets.api;

import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import org.sonar.plugins.secrets.configuration.model.matching.Matching;
import org.sonar.plugins.secrets.configuration.model.matching.filter.HeuristicsFilter;
import org.sonar.plugins.secrets.configuration.model.matching.filter.PostModule;
import org.sonar.plugins.secrets.configuration.model.matching.filter.StatisticalFilter;

public class PostFilterFactory {

  private PostFilterFactory() {
  }

  public static Predicate<String> createPredicate(@Nullable PostModule post, @Nullable Matching matching) {
    Predicate<String> postFilter = s -> true;
    if (post != null) {
      if (post.getStatisticalFilter() != null) {
        postFilter = postFilter.and(filterForStatisticalFilter(post.getStatisticalFilter(), matching));
      }
      if (post.getPatternNot() != null) {
        postFilter = postFilter.and(filterForPatternNot(post.getPatternNot()));
      }
      if (post.getHeuristicFilter() != null) {
        postFilter = postFilter.and(filterForHeuristicsFilter(post.getHeuristicFilter()));
      }
    }
    return postFilter;
  }

  static Predicate<String> filterForPatternNot(String patternNot) {
    return candidateSecret -> {
      Matcher matcher = Pattern.compile(patternNot).matcher(candidateSecret);
      return !matcher.find();
    };
  }

  static Predicate<String> filterForStatisticalFilter(StatisticalFilter statisticalFilter, @Nullable Matching matching) {
    return candidateSecret -> {
      String entropyInputString = candidateSecret;
      if (statisticalFilter.getInputString() != null && matching != null) {
        entropyInputString = calculateEntropyInputBasedOnNamedGroup(statisticalFilter.getInputString(), candidateSecret, matching);
      }
      return !EntropyChecker.hasLowEntropy(entropyInputString, statisticalFilter.getThreshold());
    };
  }

  static String calculateEntropyInputBasedOnNamedGroup(String groupName, String candidateSecret, Matching matching) {
    Matcher matcher = Pattern.compile(matching.getPattern()).matcher(candidateSecret);
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
