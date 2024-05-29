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

import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.sonar.plugins.common.InputFileContext;
import org.sonar.plugins.secrets.configuration.model.matching.AuxiliaryPatternType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.plugins.common.TestUtils.inputFileContext;
import static org.sonar.plugins.secrets.configuration.deserialization.ReferenceTestModel.constructAuxiliaryPattern;

class DisjunctionMatcherTest {

  private static final PatternMatcher candidateSecretMatcher = new PatternMatcher("\\b(candidate secret)\\b");
  private static final AuxiliaryMatcher MATCHER_BEFORE = AuxiliaryMatcher.build(constructAuxiliaryPattern(AuxiliaryPatternType.PATTERN_BEFORE, "before"));
  private static final AuxiliaryMatcher MATCHER_AFTER = AuxiliaryMatcher.build(constructAuxiliaryPattern(AuxiliaryPatternType.PATTERN_AFTER, "after"));

  @ParameterizedTest
  // @ValueSource(strings = {"candidate secret after", "before candidate secret", "before candidate secret after"})
  @ValueSource(strings = {"candidate secret after"})
  void disjunctionMatcherShouldNotRemoveCandidateSecret(String content) throws IOException {
    AuxiliaryPatternMatcher disjunctionMatcher = MATCHER_AFTER.or(MATCHER_BEFORE);

    List<Match> candidateSecrets = candidateSecretMatcher.findIn(content, "<test-rule-id>");

    InputFileContext inputFileContext = inputFileContext(content);
    List<Match> result = disjunctionMatcher.filter(candidateSecrets, inputFileContext, "<test-rule-id>");

    assertThat(disjunctionMatcher).isInstanceOf(DisjunctionMatcher.class);
    assertThat(result).containsExactlyElementsOf(candidateSecrets);
  }

  @Test
  void disjunctionMatcherShouldRemoveCandidateSecret() throws IOException {
    AuxiliaryPatternMatcher disjunctionMatcher = MATCHER_AFTER.or(MATCHER_BEFORE);

    String content = "candidate secret";
    List<Match> candidateSecrets = candidateSecretMatcher.findIn("candidate secret", "<test-rule-id>");

    InputFileContext inputFileContext = inputFileContext(content);
    List<Match> result = disjunctionMatcher.filter(candidateSecrets, inputFileContext, "<test-rule-id>");

    assertThat(disjunctionMatcher).isInstanceOf(DisjunctionMatcher.class);
    assertThat(result).isEmpty();
  }
}
