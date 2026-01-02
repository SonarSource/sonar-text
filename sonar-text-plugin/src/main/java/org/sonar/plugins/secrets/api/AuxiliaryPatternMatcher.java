/*
 * SonarQube Text Plugin
 * Copyright (C) 2021-2026 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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
import org.sonar.plugins.common.InputFileContext;

/**
 * Interface for all auxiliary matcher.
 */
public interface AuxiliaryPatternMatcher {

  DefaultAuxiliaryMatcher NO_FILTERING_AUXILIARY_MATCHER = new DefaultAuxiliaryMatcher();

  /**
   * Filters the list of {@link Match candidateMatches} based on the content. The actual filtering behavior is dependent on the actual implementation.
   * @param candidateMatches matches to be filtered
   * @param inputFileContext inputFileContext containing the content where the matches where found
   * @param ruleId id of the rule this matcher stems from. Useful for logging.
   * @return list of filtered matches
   */
  List<Match> filter(List<Match> candidateMatches, InputFileContext inputFileContext, String ruleId);

  /**
   * Returns a new {@link AuxiliaryPatternMatcher}, which conjuncts the results of this {@link AuxiliaryPatternMatcher} and the provided secondMatcher.
   * @param secondMatcher {@link AuxiliaryPatternMatcher} to be conjuncted with
   * @return the constructed {@link AuxiliaryPatternMatcher}
   */
  default AuxiliaryPatternMatcher and(AuxiliaryPatternMatcher secondMatcher) {
    return new ConjunctionMatcher(this, secondMatcher);
  }

  /**
   * Returns a new {@link AuxiliaryPatternMatcher}, which disjuncts the results of this {@link AuxiliaryPatternMatcher} and the provided secondMatcher.
   * @param secondMatcher {@link AuxiliaryPatternMatcher} to be disjuncted with
   * @return the constructed {@link AuxiliaryPatternMatcher}
   */
  default AuxiliaryPatternMatcher or(AuxiliaryPatternMatcher secondMatcher) {
    return new DisjunctionMatcher(this, secondMatcher);
  }

  /**
   * Returns a new {@link AuxiliaryPatternMatcher}, which negates the results of this {@link AuxiliaryPatternMatcher}.
   *
   * @return the constructed {@link AuxiliaryPatternMatcher}
   */
  default AuxiliaryPatternMatcher negate() {
    return new NegationMatcher(this);
  }

  /**
   * Default implementation of the {@link AuxiliaryPatternMatcher} which has no filtering behavior.
   */
  class DefaultAuxiliaryMatcher implements AuxiliaryPatternMatcher {

    @Override
    public List<Match> filter(List<Match> candidateMatches, InputFileContext inputFileContext, String ruleId) {
      return candidateMatches;
    }
  }
}
