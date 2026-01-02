/*
 * SonarQube Text Plugin
 * Copyright (C) 2021-2026 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.sonar.plugins.common.InputFileContext;
import org.sonar.plugins.secrets.configuration.model.matching.AuxiliaryPatternType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.plugins.common.TestUtils.inputFileContext;
import static org.sonar.plugins.secrets.configuration.deserialization.ReferenceTestModel.constructAuxiliaryPattern;

class AuxiliaryMatcherTest {

  private static final PatternMatcher candidateSecretMatcher = new PatternMatcher("\\b(candidate secret)\\b");

  @ParameterizedTest
  @MethodSource
  void auxiliaryPatternShouldBeDetectedAndCandidateSecretShouldNotBeRemoved(AuxiliaryPatternType patternType, String content,
    String auxiliaryPattern) throws IOException {
    var auxiliaryMatcher = AuxiliaryMatcher.build(constructAuxiliaryPattern(patternType, auxiliaryPattern));

    List<Match> candidateSecrets = candidateSecretMatcher.findIn(content, "<test-rule-id>");

    InputFileContext inputFileContext = inputFileContext(content);

    List<Match> result = auxiliaryMatcher.filter(candidateSecrets, inputFileContext, "<test-rule-id>");

    assertThat(result).containsExactlyElementsOf(candidateSecrets);
  }

  static Stream<Arguments> auxiliaryPatternShouldBeDetectedAndCandidateSecretShouldNotBeRemoved() {
    return Stream.of(
      Arguments.of(AuxiliaryPatternType.PATTERN_BEFORE, "auxiliaryPattern and candidate secret", "auxiliaryPattern"),
      Arguments.of(AuxiliaryPatternType.PATTERN_BEFORE, "auxiliaryPattern and candidate secret and auxiliaryPattern", "auxiliaryPattern"),
      Arguments.of(AuxiliaryPatternType.PATTERN_BEFORE, "auxiliaryPattern, auxiliaryPattern and candidate secret", "auxiliaryPattern"),
      Arguments.of(AuxiliaryPatternType.PATTERN_AFTER, "candidate secret and auxiliaryPattern", "auxiliaryPattern"),
      Arguments.of(AuxiliaryPatternType.PATTERN_AFTER, "auxiliaryPattern and candidate secret and auxiliaryPattern", "auxiliaryPattern"),
      Arguments.of(AuxiliaryPatternType.PATTERN_AFTER, "candidate secret and auxiliaryPattern, auxiliaryPattern", "auxiliaryPattern"),
      Arguments.of(AuxiliaryPatternType.PATTERN_AROUND, "auxiliaryPattern and candidate secret and auxiliaryPattern", "auxiliaryPattern"),
      Arguments.of(AuxiliaryPatternType.PATTERN_AROUND, "auxiliaryPattern, auxiliaryPattern and candidate secret and auxiliaryPattern, " +
        "auxiliaryPattern", "auxiliaryPattern"),
      Arguments.of(AuxiliaryPatternType.PATTERN_AROUND, "auxiliaryPattern and candidate secret", "auxiliaryPattern"),
      Arguments.of(AuxiliaryPatternType.PATTERN_AROUND, "auxiliaryPattern, auxiliaryPattern and candidate secret", "auxiliaryPattern"),
      Arguments.of(AuxiliaryPatternType.PATTERN_AROUND, "candidate secret and auxiliaryPattern", "auxiliaryPattern"),
      Arguments.of(AuxiliaryPatternType.PATTERN_AROUND, "candidate secret and auxiliaryPattern, auxiliaryPattern", "auxiliaryPattern"));
  }

  @ParameterizedTest
  @MethodSource
  void auxiliaryPatternShouldRemoveCandidateSecrets(AuxiliaryPatternType patternType, String content, String auxiliaryPattern) throws IOException {
    var auxiliaryMatcher = AuxiliaryMatcher.build(constructAuxiliaryPattern(patternType, auxiliaryPattern));

    List<Match> candidateSecrets = candidateSecretMatcher.findIn(content, "<test-rule-id>");

    InputFileContext inputFileContext = inputFileContext(content);

    List<Match> result = auxiliaryMatcher.filter(candidateSecrets, inputFileContext, "<test-rule-id>");

    assertThat(result).isEmpty();
  }

  static Stream<Arguments> auxiliaryPatternShouldRemoveCandidateSecrets() {
    return Stream.of(
      Arguments.of(AuxiliaryPatternType.PATTERN_BEFORE, "otherWord and candidate secret", "auxiliaryPattern"),
      Arguments.of(AuxiliaryPatternType.PATTERN_BEFORE, "word and candidate secret", "didat"),
      Arguments.of(AuxiliaryPatternType.PATTERN_BEFORE, "word and candidate secret and auxiliaryPattern", "auxiliaryPattern"),
      Arguments.of(AuxiliaryPatternType.PATTERN_AFTER, "candidate secret and other word", "auxiliaryPattern"),
      Arguments.of(AuxiliaryPatternType.PATTERN_AFTER, "word and candidate secret", "didat"),
      Arguments.of(AuxiliaryPatternType.PATTERN_AFTER, "auxiliaryPattern and candidate secret and other word", "auxiliaryPattern"),
      Arguments.of(AuxiliaryPatternType.PATTERN_AROUND, "something else and candidate secret and other word", "auxiliaryPattern"),
      Arguments.of(AuxiliaryPatternType.PATTERN_AROUND, "word and candidate secret", "didat"));
  }

  @Test
  void auxiliaryPatternShouldNotRemoveCandidateSecretsBecauseAuxPatternIsInCharacterDistance() throws IOException {
    var auxiliaryPattern = constructAuxiliaryPattern(AuxiliaryPatternType.PATTERN_AFTER, "auxPattern");
    auxiliaryPattern.setMaxCharacterDistance(200);
    var auxiliaryMatcher = AuxiliaryMatcher.build(auxiliaryPattern);

    String content = "candidate secret and candidate secret and auxPattern";
    List<Match> candidateSecrets = candidateSecretMatcher.findIn(content, "<test-rule-id>");

    InputFileContext inputFileContext = inputFileContext(content);
    List<Match> result = auxiliaryMatcher.filter(candidateSecrets, inputFileContext, "<test-rule-id>");

    assertThat(result).containsExactlyElementsOf(candidateSecrets);
  }

  @Test
  void auxiliaryPatternShouldRemoveCandidateSecretsBecauseAuxPatternIsOutOfCharacterDistance() throws IOException {
    var auxiliaryPattern = constructAuxiliaryPattern(AuxiliaryPatternType.PATTERN_AFTER, "auxPattern");
    auxiliaryPattern.setMaxCharacterDistance(2);
    var auxiliaryMatcher = AuxiliaryMatcher.build(auxiliaryPattern);

    String content = "candidate secret and candidate secret and auxPattern";
    List<Match> candidateSecrets = candidateSecretMatcher.findIn(content, "<test-rule-id>");

    InputFileContext inputFileContext = inputFileContext(content);
    List<Match> result = auxiliaryMatcher.filter(candidateSecrets, inputFileContext, "<test-rule-id>");

    assertThat(result).isEmpty();
  }

  @Test
  void auxiliaryPatternShouldRemoveOneCandidateSecretsBecauseItIsOutOfCharacterDistance() throws IOException {
    var auxiliaryPattern = constructAuxiliaryPattern(AuxiliaryPatternType.PATTERN_AFTER, "auxPattern");
    auxiliaryPattern.setMaxCharacterDistance(10);
    var auxiliaryMatcher = AuxiliaryMatcher.build(auxiliaryPattern);

    String content = "candidate secret and candidate secret and auxPattern";
    List<Match> candidateSecrets = candidateSecretMatcher.findIn(content, "<test-rule-id>");

    InputFileContext inputFileContext = inputFileContext(content);
    List<Match> result = auxiliaryMatcher.filter(candidateSecrets, inputFileContext, "<test-rule-id>");

    assertThat(result).containsExactly(candidateSecrets.get(1));
  }

  @Test
  void auxiliaryPatternShouldNotRemoveCandidateSecretsBecauseAuxPatternIsInLineDistance() throws IOException {
    var auxiliaryPattern = constructAuxiliaryPattern(AuxiliaryPatternType.PATTERN_AFTER, "auxPattern");
    auxiliaryPattern.setMaxLineDistance(0);
    var auxiliaryMatcher = AuxiliaryMatcher.build(auxiliaryPattern);

    String content = "candidate secret and candidate secret and auxPattern";
    List<Match> candidateSecrets = candidateSecretMatcher.findIn(content, "<test-rule-id>");

    InputFileContext inputFileContext = inputFileContext(content);
    List<Match> result = auxiliaryMatcher.filter(candidateSecrets, inputFileContext, "<test-rule-id>");

    assertThat(result).containsExactlyElementsOf(candidateSecrets);
  }

  @ParameterizedTest
  @CsvSource(value = {
    "0,0",
    "1,0",
    "2,1",
    "3,1",
    "4,2",
    "5,2"
  })
  void auxiliaryPatternShouldFilterCandidateSecretsAccordingToLineDistance(int maxLineDistance, int expectedMatches) throws IOException {
    var auxiliaryPattern = constructAuxiliaryPattern(AuxiliaryPatternType.PATTERN_AFTER, "auxPattern");
    auxiliaryPattern.setMaxLineDistance(maxLineDistance);
    var auxiliaryMatcher = AuxiliaryMatcher.build(auxiliaryPattern);

    String content = """
      content
       candidate secret
       and
       candidate secret
       and filler
       and auxPattern""";

    List<Match> candidateSecrets = candidateSecretMatcher.findIn(content, "<test-rule-id>");

    InputFileContext inputFileContext = inputFileContext(content);
    List<Match> result = auxiliaryMatcher.filter(candidateSecrets, inputFileContext, "<test-rule-id>");

    assertThat(result).hasSize(expectedMatches);
  }

  @Test
  void shouldNotMatchVeryLongLinesIfMaxLineDistanceIsSet() throws IOException {
    var auxiliaryPattern = constructAuxiliaryPattern(AuxiliaryPatternType.PATTERN_BEFORE, "auxPattern");
    auxiliaryPattern.setMaxLineDistance(1);
    var auxiliaryMatcher = AuxiliaryMatcher.build(auxiliaryPattern);

    // Line exceeds default maxLineLength (1000), so it should be filtered out
    var content = "auxPattern " + "x".repeat(1001) + " candidate secret";
    var candidateSecrets = candidateSecretMatcher.findIn(content, "<test-rule-id>");

    var inputFileContext = inputFileContext(content);
    var result = auxiliaryMatcher.filter(candidateSecrets, inputFileContext, "<test-rule-id>");

    assertThat(result).isEmpty();
  }

  @Test
  void shouldFilterOutLinesExceedingMaxLineLength() throws IOException {
    var auxiliaryPattern = constructAuxiliaryPattern(AuxiliaryPatternType.PATTERN_BEFORE, "auxPattern");
    auxiliaryPattern.setMaxLineDistance(1);
    auxiliaryPattern.setMaxLineLength(500);
    var auxiliaryMatcher = AuxiliaryMatcher.build(auxiliaryPattern);

    // Line exceeds maxLineLength (500), so it should be filtered out
    var longLineContent = "auxPattern " + "x".repeat(600) + " candidate secret";
    var candidateSecrets = candidateSecretMatcher.findIn(longLineContent, "<test-rule-id>");

    var inputFileContext = inputFileContext(longLineContent);
    var result = auxiliaryMatcher.filter(candidateSecrets, inputFileContext, "<test-rule-id>");

    assertThat(result).isEmpty();
  }

  @Test
  void shouldNotFilterOutLinesBelowMaxLineLength() throws IOException {
    var auxiliaryPattern = constructAuxiliaryPattern(AuxiliaryPatternType.PATTERN_BEFORE, "auxPattern");
    auxiliaryPattern.setMaxLineDistance(1);
    auxiliaryPattern.setMaxLineLength(500);
    var auxiliaryMatcher = AuxiliaryMatcher.build(auxiliaryPattern);

    // Line is below maxLineLength (500), so maxLineDistance check should apply normally
    var shortLineContent = "auxPattern candidate secret";
    var candidateSecrets = candidateSecretMatcher.findIn(shortLineContent, "<test-rule-id>");

    var inputFileContext = inputFileContext(shortLineContent);
    var result = auxiliaryMatcher.filter(candidateSecrets, inputFileContext, "<test-rule-id>");

    assertThat(result).containsExactlyElementsOf(candidateSecrets);
  }

}
