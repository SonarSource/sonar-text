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
import org.sonar.plugins.secrets.configuration.model.matching.AuxiliaryPatternType;

import static org.assertj.core.api.Assertions.assertThat;

class NegationMatcherTest {

  private static final PatternMatcher candidateSecretMatcher = new PatternMatcher("\\b(candidate secret)\\b");

  @Test
  void shouldRemoveCandidateSecretsBeforeAndAfterAround() {
    AuxiliaryMatcher auxiliaryMatcherBefore = new AuxiliaryMatcher(AuxiliaryPatternType.PATTERN_AROUND, new PatternMatcher("\\b(around)\\b"), Integer.MAX_VALUE);
    AuxiliaryPatternMatcher negationMatcher = auxiliaryMatcherBefore.negate();

    String content = "candidate secret another candidate secret";
    List<Match> candidateSecrets = candidateSecretMatcher.findIn(content, "<test-rule-id>");

    List<Match> result = negationMatcher.filter(candidateSecrets, content, "<test-rule-id>");

    assertThat(negationMatcher).isInstanceOf(NegationMatcher.class);
    assertThat(result).containsExactlyElementsOf(candidateSecrets);
  }

  @Test
  void shouldNotRemoveCandidateSecretsMatchEither() {
    AuxiliaryMatcher auxiliaryMatcherBefore = new AuxiliaryMatcher(AuxiliaryPatternType.PATTERN_BEFORE, new PatternMatcher("\\b(before)\\b"), Integer.MAX_VALUE);
    AuxiliaryMatcher auxiliaryMatcherAfter = new AuxiliaryMatcher(AuxiliaryPatternType.PATTERN_AFTER, new PatternMatcher("\\b(after)\\b"), Integer.MAX_VALUE);
    AuxiliaryPatternMatcher conjunctionMatcher = auxiliaryMatcherAfter.and(auxiliaryMatcherBefore);

    AuxiliaryPatternMatcher negationMatcher = conjunctionMatcher.negate();
    String content = "candidate secret around another candidate secret after";
    List<Match> candidateSecrets = candidateSecretMatcher.findIn(content, "<test-rule-id>");

    List<Match> result = negationMatcher.filter(candidateSecrets, content, "<test-rule-id>");

    assertThat(negationMatcher).isInstanceOf(NegationMatcher.class);
    assertThat(result).containsExactlyElementsOf(candidateSecrets);
  }

  @Test
  void shouldRemoveCandidateSecretsMatchEither() {
    AuxiliaryMatcher auxiliaryMatcherBefore = new AuxiliaryMatcher(AuxiliaryPatternType.PATTERN_BEFORE, new PatternMatcher("\\b(before)\\b"), Integer.MAX_VALUE);
    AuxiliaryMatcher auxiliaryMatcherAfter = new AuxiliaryMatcher(AuxiliaryPatternType.PATTERN_AFTER, new PatternMatcher("\\b(after)\\b"), Integer.MAX_VALUE);
    AuxiliaryPatternMatcher disjunctionMatcher = auxiliaryMatcherAfter.or(auxiliaryMatcherBefore);

    AuxiliaryPatternMatcher negationMatcher = disjunctionMatcher.negate();
    String content = "candidate secret before another candidate secret";
    List<Match> candidateSecrets = candidateSecretMatcher.findIn(content, "<test-rule-id>");

    List<Match> result = negationMatcher.filter(candidateSecrets, content, "<test-rule-id>");

    assertThat(negationMatcher).isInstanceOf(NegationMatcher.class);
    assertThat(result)
      .hasSize(1)
      .contains(result.get(0));
  }

  @Test
  void shouldRemoveMultipleCandidateSecrets() {
    AuxiliaryMatcher auxiliaryMatcherBefore = new AuxiliaryMatcher(AuxiliaryPatternType.PATTERN_BEFORE, new PatternMatcher("\\b(before)\\b"), Integer.MAX_VALUE);
    AuxiliaryPatternMatcher negationMatcher = auxiliaryMatcherBefore.negate();

    String content = "before candidate secret after another candidate secret";
    List<Match> candidateSecrets = candidateSecretMatcher.findIn(content, "<test-rule-id>");

    List<Match> result = negationMatcher.filter(candidateSecrets, content, "<test-rule-id>");

    assertThat(negationMatcher).isInstanceOf(NegationMatcher.class);
    assertThat(result).isEmpty();
  }

  @Test
  void shouldReturnOnlyOneCandidateSecretLeftAfterNegation() {
    AuxiliaryMatcher auxiliaryMatcherBefore = new AuxiliaryMatcher(AuxiliaryPatternType.PATTERN_BEFORE, new PatternMatcher("\\b(before)\\b"), Integer.MAX_VALUE);
    AuxiliaryPatternMatcher negationMatcher = auxiliaryMatcherBefore.negate();

    String content = "candidate secret before candidate secret after";
    List<Match> candidateSecrets = candidateSecretMatcher.findIn(content, "<test-rule-id>");

    List<Match> result = negationMatcher.filter(candidateSecrets, content, "<test-rule-id>");

    assertThat(negationMatcher).isInstanceOf(NegationMatcher.class);
    assertThat(result)
      .hasSize(1)
      .contains(result.get(0));
  }

  @Test
  void shouldReturnAllCandidateSecretWhenNoMatches() {
    AuxiliaryMatcher auxiliaryMatcherBefore = new AuxiliaryMatcher(AuxiliaryPatternType.PATTERN_BEFORE, new PatternMatcher("\\b(before)\\b"), Integer.MAX_VALUE);
    AuxiliaryPatternMatcher negationMatcher = auxiliaryMatcherBefore.negate();

    String content = "candidate secret after another candidate secret";
    List<Match> candidateSecrets = candidateSecretMatcher.findIn(content, "<test-rule-id>");

    List<Match> result = negationMatcher.filter(candidateSecrets, content, "<test-rule-id>");

    assertThat(negationMatcher).isInstanceOf(NegationMatcher.class);
    assertThat(result).containsExactlyElementsOf(candidateSecrets);
  }
}
