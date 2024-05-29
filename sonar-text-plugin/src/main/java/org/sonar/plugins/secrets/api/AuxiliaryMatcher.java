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

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;
import org.sonar.plugins.common.InputFileContext;
import org.sonar.plugins.secrets.configuration.model.matching.AuxiliaryPattern;
import org.sonar.plugins.secrets.configuration.model.matching.AuxiliaryPatternType;

import static org.sonar.plugins.secrets.api.DistanceValidation.inDistanceOf;
import static org.sonar.plugins.secrets.api.DistanceValidation.isAfter;
import static org.sonar.plugins.secrets.api.DistanceValidation.isBefore;

/**
 * Matcher for auxiliary patterns, which can be found in the context of candidate secrets.
 */
public class AuxiliaryMatcher implements AuxiliaryPatternMatcher {
  private final AuxiliaryPattern auxiliaryPattern;
  private final PatternMatcher auxiliaryPatternMatcher;

  AuxiliaryMatcher(AuxiliaryPattern auxiliaryPattern, PatternMatcher auxiliaryPatternMatcher) {
    this.auxiliaryPattern = auxiliaryPattern;
    this.auxiliaryPatternMatcher = auxiliaryPatternMatcher;
  }

  /**
   * Creates a {@link AuxiliaryMatcher} based on the provided {@link AuxiliaryPattern}.
   * @param auxiliaryPattern the input {@link AuxiliaryPattern}
   * @return the constructed {@link AuxiliaryMatcher}
   */
  public static AuxiliaryMatcher build(AuxiliaryPattern auxiliaryPattern) {
    return new AuxiliaryMatcher(auxiliaryPattern, PatternMatcher.build(auxiliaryPattern));
  }

  @Override
  public List<Match> filter(List<Match> candidateMatches, InputFileContext inputFileContext, String ruleId) {
    if (AuxiliaryPatternType.PATTERN_NOT == auxiliaryPattern.getType()) {
      // Not supported at the moment, so we don't filter anything
      // SONARTEXT-54: Implement missing features of detection logic
      return candidateMatches;
    }

    List<Match> auxiliaryMatches = auxiliaryPatternMatcher.findIn(inputFileContext.content(), ruleId);
    if (auxiliaryMatches.isEmpty()) {
      return new ArrayList<>();
    }
    BiPredicate<Match, Match> comparisonFunction = createComparisonFunction(inputFileContext);
    return filterBasedOnFunction(candidateMatches, auxiliaryMatches, comparisonFunction);
  }

  private BiPredicate<Match, Match> createComparisonFunction(InputFileContext inputFileContext) {
    BiPredicate<Match, Match> result;
    if (AuxiliaryPatternType.PATTERN_BEFORE == auxiliaryPattern.getType()) {
      result = DistanceValidation::isBefore;
    } else if (AuxiliaryPatternType.PATTERN_AFTER == auxiliaryPattern.getType()) {
      result = DistanceValidation::isAfter;
    } else {
      result = (auxMatch, candidateMatch) -> isBefore(auxMatch, candidateMatch) || isAfter(auxMatch, candidateMatch);
    }

    if (auxiliaryPattern.getMaxCharacterDistance() != null) {
      result = result.and((auxMatch, candidateMatch) -> inDistanceOf(auxMatch, candidateMatch, auxiliaryPattern.getMaxCharacterDistance()));
    }
    if (auxiliaryPattern.getMaxLineDistance() != null) {
      result = result.and((auxMatch, candidateMatch) -> {
        int auxMatchStartLine = inputFileContext.offsetToLineNumber(auxMatch.getFileStartOffset());
        int auxMatchEndLine = inputFileContext.offsetToLineNumber(auxMatch.getFileEndOffset());
        int candidateMatchStartLine = inputFileContext.offsetToLineNumber(candidateMatch.getFileStartOffset());
        int candidateMatchEndLine = inputFileContext.offsetToLineNumber(candidateMatch.getFileEndOffset());

        return inDistanceOf(auxMatchStartLine, auxMatchEndLine, candidateMatchStartLine, candidateMatchEndLine, auxiliaryPattern.getMaxLineDistance());
      });
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
