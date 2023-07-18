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
import org.sonar.plugins.secrets.configuration.model.matching.AuxiliaryPattern;
import org.sonar.plugins.secrets.configuration.model.matching.AuxiliaryPatternType;

public class AuxiliaryMatcher implements AuxiliaryPatternMatcher {

  private final AuxiliaryPatternType type;
  private final PatternMatcher auxiliaryPatternMatcher;

  AuxiliaryMatcher(AuxiliaryPatternType type, PatternMatcher auxiliaryPatternMatcher) {
    this.type = type;
    this.auxiliaryPatternMatcher = auxiliaryPatternMatcher;
  }

  public static AuxiliaryMatcher build(AuxiliaryPattern auxiliaryPattern) {
    return new AuxiliaryMatcher(auxiliaryPattern.getType(), PatternMatcher.build(auxiliaryPattern));
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
    return filterBasedOnType(candidateMatches, auxiliaryMatches);
  }

  private List<Match> filterBasedOnType(List<Match> candidateMatches, List<Match> auxiliaryMatches) {
    if (AuxiliaryPatternType.PATTERN_BEFORE == type) {
      return filterForBefore(candidateMatches, auxiliaryMatches);
    } else if (AuxiliaryPatternType.PATTERN_AFTER == type) {
      return filterForAfter(candidateMatches, auxiliaryMatches);
    } else {
      return filterForAround(candidateMatches, auxiliaryMatches);
    }
  }

  private List<Match> filterForAfter(List<Match> candidateMatches, List<Match> auxiliaryMatches) {
    List<Match> filteredCandidates = new ArrayList<>();

    for (Match regexMatch : candidateMatches) {
      // since we are searching for after, the last one (position wise) is enough
      Match lastAuxMatch = auxiliaryMatches.get(auxiliaryMatches.size() - 1);
      if (lastAuxMatch.isAfter(regexMatch)) {
        filteredCandidates.add(regexMatch);
      }
    }
    return filteredCandidates;
  }

  private List<Match> filterForAround(List<Match> candidateMatches, List<Match> auxiliaryMatches) {
    List<Match> filteredCandidates = new ArrayList<>();

    for (Match candidate : candidateMatches) {
      Match lastAuxMatch = auxiliaryMatches.get(auxiliaryMatches.size() - 1);
      Match firstAux = auxiliaryMatches.get(0);
      if (lastAuxMatch.isAfter(candidate) || firstAux.isBefore(candidate)) {
        filteredCandidates.add(candidate);
      }
    }
    return filteredCandidates;
  }

  private List<Match> filterForBefore(List<Match> candidateMatches, List<Match> auxiliaryMatches) {
    List<Match> filteredCandidates = new ArrayList<>();

    for (Match candidate : candidateMatches) {
      // since we are searching for before, first one (position wise) is enough
      Match firstAux = auxiliaryMatches.get(0);
      if (firstAux.isBefore(candidate)) {
        filteredCandidates.add(candidate);
      }
    }
    return filteredCandidates;
  }
}
