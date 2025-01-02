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

import java.util.List;
import org.sonar.plugins.common.InputFileContext;

/**
 * A {@link AuxiliaryPatternMatcher} which disjuncts both filtering results of the provided {@link AuxiliaryPatternMatcher}.
 */
public class DisjunctionMatcher implements AuxiliaryPatternMatcher {

  private final AuxiliaryPatternMatcher matcherLeft;
  private final AuxiliaryPatternMatcher matcherRight;

  /**
   * Creates a new {@link DisjunctionMatcher}.
   * @param matcherLeft first {@link AuxiliaryPatternMatcher}
   * @param matcherRight second {@link AuxiliaryPatternMatcher}
   */
  public DisjunctionMatcher(AuxiliaryPatternMatcher matcherLeft, AuxiliaryPatternMatcher matcherRight) {
    this.matcherLeft = matcherLeft;
    this.matcherRight = matcherRight;
  }

  @Override
  public List<Match> filter(List<Match> regexMatch, InputFileContext inputFileContext, String ruleId) {
    List<Match> matchesLeft = matcherLeft.filter(regexMatch, inputFileContext, ruleId);
    matchesLeft.addAll(matcherRight.filter(regexMatch, inputFileContext, ruleId));
    return matchesLeft;
  }
}
