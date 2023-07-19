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

public interface AuxiliaryPatternMatcher {

  DefaultAuxiliaryMatcher NO_FILTERING_AUXILIARY_MATCHER = new DefaultAuxiliaryMatcher();

  List<Match> filter(List<Match> candidateMatches, String content);

  default AuxiliaryPatternMatcher and(AuxiliaryPatternMatcher secondMatcher) {
    return new ConjunctionMatcher(this, secondMatcher);
  }

  default AuxiliaryPatternMatcher or(AuxiliaryPatternMatcher secondMatcher) {
    return new DisjunctionMatcher(this, secondMatcher);
  }

  class DefaultAuxiliaryMatcher implements AuxiliaryPatternMatcher {

    @Override
    public List<Match> filter(List<Match> candidateMatches, String content) {
      return candidateMatches;
    }
  }
}
