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
import org.sonar.plugins.common.InputFileContext;

/**
 * A {@link AuxiliaryPatternMatcher} which conjuncts both filtering results of the provided {@link AuxiliaryPatternMatcher}.
 */
public class ConjunctionMatcher implements AuxiliaryPatternMatcher {

  private final AuxiliaryPatternMatcher matcherLeft;
  private final AuxiliaryPatternMatcher matcherRight;

  /**
   * Creates a new {@link ConjunctionMatcher}.
   * @param matcherLeft first {@link AuxiliaryPatternMatcher}
   * @param matcherRight second {@link AuxiliaryPatternMatcher}
   */
  public ConjunctionMatcher(AuxiliaryPatternMatcher matcherLeft, AuxiliaryPatternMatcher matcherRight) {
    this.matcherLeft = matcherLeft;
    this.matcherRight = matcherRight;
  }

  @Override
  public List<Match> filter(List<Match> regexMatch, InputFileContext inputFileContext, String ruleId) {
    List<Match> matchesLeft = matcherLeft.filter(regexMatch, inputFileContext, ruleId);
    List<Match> matchesRight = matcherRight.filter(regexMatch, inputFileContext, ruleId);
    matchesRight.retainAll(matchesLeft);
    return matchesRight;
  }
}
