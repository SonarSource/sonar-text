/*
 * SonarQube Text Plugin
 * Copyright (C) 2021-2024 SonarSource SA
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

import java.util.List;
import javax.annotation.Nullable;
import org.sonar.plugins.secrets.configuration.model.matching.AuxiliaryPattern;
import org.sonar.plugins.secrets.configuration.model.matching.BooleanCombination;
import org.sonar.plugins.secrets.configuration.model.matching.BooleanCombinationType;
import org.sonar.plugins.secrets.configuration.model.matching.Match;
import org.sonar.plugins.secrets.configuration.model.matching.Matching;

public class AuxiliaryPatternMatcherFactory {

  private AuxiliaryPatternMatcherFactory() {
  }

  public static AuxiliaryPatternMatcher build(@Nullable Matching matching) {
    if (matching == null || matching.getContext() == null) {
      return AuxiliaryPatternMatcher.NO_FILTERING_AUXILIARY_MATCHER;
    }
    return constructFrom(matching.getContext());
  }

  private static AuxiliaryPatternMatcher constructFrom(Match match) {
    if (match instanceof BooleanCombination) {
      return constructFrom((BooleanCombination) match);
    } else {
      return constructFrom((AuxiliaryPattern) match);
    }
  }

  private static AuxiliaryPatternMatcher constructFrom(BooleanCombination booleanCombination) {
    List<Match> matches = booleanCombination.getMatches();
    AuxiliaryPatternMatcher resultingMatcher = constructFrom(matches.get(0));
    for (int i = 1; i < matches.size(); i++) {
      AuxiliaryPatternMatcher matcher = constructFrom(matches.get(i));
      if (BooleanCombinationType.MATCH_EACH == booleanCombination.getType()) {
        resultingMatcher = resultingMatcher.and(matcher);
      } else {
        resultingMatcher = resultingMatcher.or(matcher);
      }
    }
    return resultingMatcher;
  }

  private static AuxiliaryPatternMatcher constructFrom(AuxiliaryPattern auxiliaryPattern) {
    return AuxiliaryMatcher.build(auxiliaryPattern);
  }
}
