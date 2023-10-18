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

import java.util.List;

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
  public List<Match> filter(List<Match> regexMatch, String content, String ruleId) {
    List<Match> matchesLeft = matcherLeft.filter(regexMatch, content, ruleId);
    matchesLeft.addAll(matcherRight.filter(regexMatch, content, ruleId));
    return matchesLeft;
  }
}
