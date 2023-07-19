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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.sonar.plugins.secrets.configuration.model.matching.AuxiliaryPatternType;

import static org.assertj.core.api.Assertions.assertThat;

class DisjunctionMatcherTest {

  private static final PatternMatcher candidateSecretMatcher = new PatternMatcher("\\b(candidate secret)\\b");

  @ParameterizedTest
  //@ValueSource(strings = {"candidate secret after", "before candidate secret", "before candidate secret after"})
  @ValueSource(strings = {"candidate secret after"})
  void conjunctionMatcherShouldNotRemoveCandidateSecret(String content) {
    AuxiliaryMatcher auxiliaryMatcherBefore = new AuxiliaryMatcher(
      AuxiliaryPatternType.PATTERN_BEFORE, new PatternMatcher("\\b(before)\\b"));

    AuxiliaryMatcher auxiliaryMatcherAfter = new AuxiliaryMatcher(
      AuxiliaryPatternType.PATTERN_AFTER, new PatternMatcher("\\b(after)\\b"));
    AuxiliaryPatternMatcher disjunctionMatcher = auxiliaryMatcherAfter.or(auxiliaryMatcherBefore);

    List<Match> candidateSecrets = candidateSecretMatcher.findIn(content);

    List<Match> result = disjunctionMatcher.filter(candidateSecrets, content);

    assertThat(disjunctionMatcher).isInstanceOf(DisjunctionMatcher.class);
    assertThat(result).containsExactlyElementsOf(candidateSecrets);
  }

  @Test
  void conjunctionMatcherShouldRemoveCandidateSecret() {
    AuxiliaryMatcher auxiliaryMatcherBefore = new AuxiliaryMatcher(
      AuxiliaryPatternType.PATTERN_BEFORE, new PatternMatcher("\\b(before)\\b"));

    AuxiliaryMatcher auxiliaryMatcherAfter = new AuxiliaryMatcher(
      AuxiliaryPatternType.PATTERN_AFTER, new PatternMatcher("\\b(after)\\b"));
    AuxiliaryPatternMatcher disjunctionMatcher = auxiliaryMatcherAfter.or(auxiliaryMatcherBefore);

    String content = "candidate secret";
    List<Match> candidateSecrets = candidateSecretMatcher.findIn("candidate secret");

    List<Match> result = disjunctionMatcher.filter(candidateSecrets, content);

    assertThat(disjunctionMatcher).isInstanceOf(DisjunctionMatcher.class);
    assertThat(result).isEmpty();
  }
}
