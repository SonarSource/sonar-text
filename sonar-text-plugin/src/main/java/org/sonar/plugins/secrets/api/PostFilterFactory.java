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
import org.sonar.plugins.secrets.configuration.model.matching.filter.PostModule;
import org.sonar.plugins.secrets.configuration.model.matching.filter.StatisticalFilter;

public class PostFilterFactory {

  private PostFilterFactory() {
  }

  public static Predicate<String> createPredicate(@Nullable PostModule post) {
    Predicate<String> postFilter = s -> true;
    if (post != null) {
      if (post.getStatisticalFilter() != null) {
        postFilter = postFilter.and(filterForStatisticalFilter(post.getStatisticalFilter()));
      }
      if (post.getPatternNot() != null) {
        postFilter = postFilter.and(filterForPatternNot(post.getPatternNot()));
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

  static Predicate<String> filterForStatisticalFilter(StatisticalFilter statisticalFilter) {
    return candidateSecret -> !EntropyChecker.hasLowEntropy(candidateSecret, statisticalFilter.getThreshold());
  }

}
