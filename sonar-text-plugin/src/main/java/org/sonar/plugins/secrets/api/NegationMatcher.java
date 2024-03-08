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
import java.util.stream.Collectors;

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
  public List<Match> filter(List<Match> candidateMatches, String content, String ruleId) {
    List<Match> matches = matcher.filter(candidateMatches, content, ruleId);
    return candidateMatches.stream()
      .filter(candidate -> !matches.contains(candidate))
      .collect(Collectors.toList());
  }
}
