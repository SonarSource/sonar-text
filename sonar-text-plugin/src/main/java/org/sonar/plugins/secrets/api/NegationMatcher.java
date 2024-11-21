/*
 * SonarQube Text Plugin
 * Copyright (C) 2021-2024 SonarSource SA
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
import java.util.stream.Collectors;
import org.sonar.plugins.common.InputFileContext;

/**
 * A {@link AuxiliaryPatternMatcher} which negates the matches found by another {@link AuxiliaryPatternMatcher}.
 */
public class NegationMatcher implements AuxiliaryPatternMatcher {
  private final AuxiliaryPatternMatcher matcher;

  /**
   * Creates a new {@link NegationMatcher}.
   * @param matcher {@link AuxiliaryPatternMatcher} to be negated
   */
  public NegationMatcher(AuxiliaryPatternMatcher matcher) {
    this.matcher = matcher;
  }

  @Override
  public List<Match> filter(List<Match> candidateMatches, InputFileContext inputFileContext, String ruleId) {
    List<Match> matches = matcher.filter(candidateMatches, inputFileContext, ruleId);
    return candidateMatches.stream()
      .filter(candidate -> !matches.contains(candidate))
      .collect(Collectors.toList());
  }
}
