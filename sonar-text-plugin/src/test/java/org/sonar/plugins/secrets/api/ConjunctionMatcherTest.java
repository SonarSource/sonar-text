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

class ConjunctionMatcherTest {

  private static final PatternMatcher candidateSecretMatcher = new PatternMatcher("\\b(candidate secret)\\b");
  private static final AuxiliaryMatcher MATCHER_BEFORE = AuxiliaryMatcher.build(constructAuxiliaryPattern(AuxiliaryPatternType.PATTERN_BEFORE, "before"));
  private static final AuxiliaryMatcher MATCHER_AFTER = AuxiliaryMatcher.build(constructAuxiliaryPattern(AuxiliaryPatternType.PATTERN_AFTER, "after"));

  @Test
  void conjunctionMatcherShouldNotRemoveCandidateSecret() throws IOException {
    AuxiliaryPatternMatcher conjunctionMatcher = MATCHER_AFTER.and(MATCHER_BEFORE);

    String content = "before candidate secret after";
    List<Match> candidateSecrets = candidateSecretMatcher.findIn(content, "<test-rule-id>");

    InputFileContext inputFileContext = inputFileContext(content);
    List<Match> result = conjunctionMatcher.filter(candidateSecrets, inputFileContext, "<test-rule-id>");

    assertThat(conjunctionMatcher).isInstanceOf(ConjunctionMatcher.class);
    assertThat(result).containsExactlyElementsOf(candidateSecrets);
  }

  @ParameterizedTest
  @ValueSource(strings = {"candidate secret after", "before candidate secret", "candidate secret"})
  void conjunctionMatcherShouldRemoveCandidateSecret(String content) throws IOException {
    AuxiliaryPatternMatcher conjunctionMatcher = MATCHER_AFTER.and(MATCHER_BEFORE);

    List<Match> candidateSecrets = candidateSecretMatcher.findIn(content, "<test-rule-id>");

    InputFileContext inputFileContext = inputFileContext(content);
    List<Match> result = conjunctionMatcher.filter(candidateSecrets, inputFileContext, "<test-rule-id>");

    assertThat(conjunctionMatcher).isInstanceOf(ConjunctionMatcher.class);
    assertThat(result).isEmpty();
  }

}
