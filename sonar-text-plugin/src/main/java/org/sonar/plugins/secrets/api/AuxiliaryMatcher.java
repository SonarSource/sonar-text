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

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;

import org.sonar.plugins.secrets.configuration.model.matching.AuxiliaryPattern;
import org.sonar.plugins.secrets.configuration.model.matching.AuxiliaryPatternType;

public class AuxiliaryMatcher implements AuxiliaryPatternMatcher {

  private final AuxiliaryPatternType type;
  private final PatternMatcher auxiliaryPatternMatcher;
  private final Integer maxDistance;

  AuxiliaryMatcher(AuxiliaryPatternType type, PatternMatcher auxiliaryPatternMatcher, int maxDistance) {
    this.type = type;
    this.auxiliaryPatternMatcher = auxiliaryPatternMatcher;
    this.maxDistance = maxDistance;
  }

  public static AuxiliaryMatcher build(AuxiliaryPattern auxiliaryPattern) {
    int maxDistance = Integer.MAX_VALUE;
    if (auxiliaryPattern.getMaxCharacterDistance() != null) {
      maxDistance = auxiliaryPattern.getMaxCharacterDistance();
    }
    return new AuxiliaryMatcher(auxiliaryPattern.getType(), PatternMatcher.build(auxiliaryPattern), maxDistance);
  }

  public List<Match> filter(List<Match> candidateMatches, String content) {
    if (AuxiliaryPatternType.PATTERN_NOT == type) {
      // Not supported at the moment, so we don't filter anything
      // SONARTEXT-54: Implement missing features of detection logic
      return candidateMatches;
    }
    List<Match> auxiliaryMatches = auxiliaryPatternMatcher.findIn(content);
    if (auxiliaryMatches.isEmpty()) {
      return new ArrayList<>();
    }
    BiPredicate<Match, Match> comparisonFunction = createComparisonFunction();
    return filterBasedOnFunction(candidateMatches, auxiliaryMatches, comparisonFunction);
  }

  private BiPredicate<Match, Match> createComparisonFunction() {
    BiPredicate<Match, Match> result;
    if (AuxiliaryPatternType.PATTERN_BEFORE == type) {
      result = Match::isBefore;
    } else if (AuxiliaryPatternType.PATTERN_AFTER == type) {
      result = Match::isAfter;
    } else {
      result = (auxMatch, candidateMatch) -> auxMatch.isBefore(candidateMatch) || auxMatch.isAfter(candidateMatch);
    }

    if (maxDistance != Integer.MAX_VALUE) {
      result = result.and((auxMatch, candidateMatch) -> auxMatch.inDistanceOf(candidateMatch, maxDistance));
    }
    return result;
  }

  private static List<Match> filterBasedOnFunction(List<Match> candidateMatches, List<Match> auxiliaryMatches, BiPredicate<Match, Match> comparisonFunction) {
    List<Match> filteredCandidates = new ArrayList<>();

    for (Match regexMatch : candidateMatches) {
      for (Match auxiliaryMatch : auxiliaryMatches) {
        if (comparisonFunction.test(auxiliaryMatch, regexMatch)) {
          filteredCandidates.add(regexMatch);
          break;
        }
      }
    }
    return filteredCandidates;
  }
}
