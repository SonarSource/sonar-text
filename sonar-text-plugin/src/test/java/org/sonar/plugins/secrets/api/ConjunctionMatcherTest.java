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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.sonar.plugins.secrets.configuration.model.matching.AuxiliaryPatternType;

import static org.assertj.core.api.Assertions.assertThat;

class ConjunctionMatcherTest {

  private static final PatternMatcher candidateSecretMatcher = new PatternMatcher("\\b(candidate secret)\\b");

  @Test
  void conjunctionMatcherShouldNotRemoveCandidateSecret() {
    AuxiliaryMatcher auxiliaryMatcherBefore = new AuxiliaryMatcher(
      AuxiliaryPatternType.PATTERN_BEFORE, new PatternMatcher("\\b(before)\\b"), Integer.MAX_VALUE);

    AuxiliaryMatcher auxiliaryMatcherAfter = new AuxiliaryMatcher(
      AuxiliaryPatternType.PATTERN_AFTER, new PatternMatcher("\\b(after)\\b"), Integer.MAX_VALUE);
    AuxiliaryPatternMatcher conjunctionMatcher = auxiliaryMatcherAfter.and(auxiliaryMatcherBefore);

    String content = "before candidate secret after";
    List<Match> candidateSecrets = candidateSecretMatcher.findIn(content, "<test-rule-id>");

    List<Match> result = conjunctionMatcher.filter(candidateSecrets, content, "<test-rule-id>");

    assertThat(conjunctionMatcher).isInstanceOf(ConjunctionMatcher.class);
    assertThat(result).containsExactlyElementsOf(candidateSecrets);
  }

  @ParameterizedTest
  @ValueSource(strings = {"candidate secret after", "before candidate secret", "candidate secret"})
  void conjunctionMatcherShouldRemoveCandidateSecret(String content) {
    AuxiliaryMatcher auxiliaryMatcherBefore = new AuxiliaryMatcher(
      AuxiliaryPatternType.PATTERN_BEFORE, new PatternMatcher("\\b(before)\\b"), Integer.MAX_VALUE);

    AuxiliaryMatcher auxiliaryMatcherAfter = new AuxiliaryMatcher(
      AuxiliaryPatternType.PATTERN_AFTER, new PatternMatcher("\\b(after)\\b"), Integer.MAX_VALUE);
    AuxiliaryPatternMatcher conjunctionMatcher = auxiliaryMatcherAfter.and(auxiliaryMatcherBefore);

    List<Match> candidateSecrets = candidateSecretMatcher.findIn(content, "<test-rule-id>");

    List<Match> result = conjunctionMatcher.filter(candidateSecrets, content, "<test-rule-id>");

    assertThat(conjunctionMatcher).isInstanceOf(ConjunctionMatcher.class);
    assertThat(result).isEmpty();
  }

}
