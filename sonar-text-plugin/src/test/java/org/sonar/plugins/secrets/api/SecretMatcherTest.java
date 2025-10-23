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
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.eclipse.jgit.util.Base64;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.plugins.secrets.configuration.deserialization.ReferenceTestModel;
import org.sonar.plugins.secrets.configuration.model.Rule;
import org.sonar.plugins.secrets.configuration.model.RuleScope;
import org.sonar.plugins.secrets.configuration.model.Selectivity;
import org.sonar.plugins.secrets.configuration.model.matching.filter.PreModule;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.plugins.common.TestUtils.inputFile;
import static org.sonar.plugins.common.TestUtils.inputFileContext;
import static org.sonar.plugins.common.TestUtils.mockDurationStatistics;
import static org.sonar.plugins.secrets.api.AuxiliaryPatternMatcherFactoryTest.constructReferenceAuxiliaryMatcher;
import static org.sonar.plugins.secrets.api.SecretMatcherAssert.assertThat;
import static org.sonar.plugins.secrets.configuration.model.RuleScope.MAIN;
import static org.sonar.plugins.secrets.configuration.model.RuleScope.TEST;

class SecretMatcherTest {

  @Test
  void testConstructionOfSimpleDetection() {
    Rule rule = ReferenceTestModel.constructMinimumSpecification().getProvider().getRules().get(0);

    PatternMatcher patternMatcher = new PatternMatcher("\\b(rule matching pattern)\\b");
    SecretMatcher expectedMatcher = new SecretMatcher(
      rule.getId(),
      rule.getMetadata().getMessage(),
      rule.getSelectivity(),
      patternMatcher,
      AuxiliaryPatternMatcher.NO_FILTERING_AUXILIARY_MATCHER,
      file -> true,
      s -> true,
      emptyMap(),
      mockDurationStatistics());

    SecretMatcher actualMatcher = SecretMatcher.build(rule, mockDurationStatistics(), SpecificationConfiguration.AUTO_TEST_FILE_DETECTION_ENABLED, true);

    assertThat(actualMatcher).behavesLike(expectedMatcher);
  }

  @Test
  void testConstructionOfDetectionWithPostFilter() {
    Rule rule = ReferenceTestModel.constructReferenceSpecification().getProvider().getRules().get(0);

    PatternMatcher patternMatcher = new PatternMatcher("\\b(rule matching pattern)\\b");

    Predicate<String> expectedPredicate = s -> true;
    Predicate<String> patternNotFilter = candidateSecret -> {
      Matcher matcher = Pattern.compile("EXAMPLEKEY").matcher(candidateSecret);
      return !matcher.find();
    };
    Predicate<String> statisticalFilter = candidateSecret -> !EntropyChecker.hasLowEntropy(candidateSecret, 4.2f);
    expectedPredicate = expectedPredicate.and(statisticalFilter).and(patternNotFilter);
    var expectedGroupPredicate = (Predicate<String>) candidateSecret -> {
      String base64Decoded = null;
      try {
        base64Decoded = new String(Base64.decode(candidateSecret), StandardCharsets.UTF_8);
      } catch (Exception e) {
      }
      return !Heuristics.matchesHeuristics(candidateSecret, List.of("uri")) && base64Decoded != null && base64Decoded.equals("\"alg\":");
    };
    SecretMatcher expectedMatcher = new SecretMatcher(
      rule.getId(),
      rule.getMetadata().getMessage(),
      rule.getSelectivity(),
      patternMatcher,
      constructReferenceAuxiliaryMatcher(),
      file -> true,
      expectedPredicate,
      Map.of("groupName", expectedGroupPredicate),
      mockDurationStatistics());

    SecretMatcher actualMatcher = SecretMatcher.build(rule, mockDurationStatistics(), SpecificationConfiguration.AUTO_TEST_FILE_DETECTION_ENABLED, true);

    assertThat(actualMatcher).behavesLike(expectedMatcher);
  }

  @Test
  void testConstructionOfDetectionWithoutMatching() {
    Rule rule = ReferenceTestModel.constructMinimumSpecification().getProvider().getRules().get(0);
    rule.getDetection().setMatching(null);
    PatternMatcher patternMatcher = new PatternMatcher(null);
    SecretMatcher expectedMatcher = new SecretMatcher(
      rule.getId(),
      rule.getMetadata().getMessage(),
      rule.getSelectivity(),
      patternMatcher,
      AuxiliaryPatternMatcher.NO_FILTERING_AUXILIARY_MATCHER,
      file -> true,
      s -> true,
      emptyMap(),
      mockDurationStatistics());

    SecretMatcher actualMatcher = SecretMatcher.build(rule, mockDurationStatistics(), SpecificationConfiguration.AUTO_TEST_FILE_DETECTION_ENABLED, true);

    assertThat(actualMatcher).behavesLike(expectedMatcher);
  }

  static List<Arguments> shouldFindIssueInMainFile() {
    return List.of(
      Arguments.of(List.of(MAIN, TEST), InputFile.Type.MAIN, 1),
      Arguments.of(List.of(MAIN, TEST), InputFile.Type.TEST, 1),
      Arguments.of(List.of(MAIN), InputFile.Type.MAIN, 1),
      Arguments.of(List.of(MAIN), InputFile.Type.TEST, 0),
      Arguments.of(List.of(TEST), InputFile.Type.MAIN, 0),
      Arguments.of(List.of(TEST), InputFile.Type.TEST, 1),
      Arguments.of(List.of(), InputFile.Type.MAIN, 1),
      Arguments.of(List.of(), InputFile.Type.TEST, 1));
  }

  @ParameterizedTest
  @MethodSource
  void shouldFindIssueInMainFile(List<RuleScope> scopesInSpec, InputFile.Type type, int expectedMatches) throws IOException {
    var specification = ReferenceTestModel.constructMinimumSpecification();
    var rule = specification.getProvider().getRules().get(0);
    var detection = rule.getDetection();
    var preModule = new PreModule();
    preModule.setScopes(scopesInSpec);
    detection.setPre(preModule);
    SecretMatcher actualMatcher = SecretMatcher.build(rule, mockDurationStatistics(), SpecificationConfiguration.AUTO_TEST_FILE_DETECTION_ENABLED, true);
    var inputFile = inputFile(Path.of(".env"), "rule matching pattern", null, type);
    var fileContext = inputFileContext(inputFile);

    var result = actualMatcher.findIn(fileContext);

    assertThat(result).hasSize(expectedMatches);
  }

  @ParameterizedTest
  @MethodSource
  void shouldRaiseIssueAccordingToSelectivityAndLanguageExistence(Selectivity ruleSelectivity, String language, boolean shouldRaise) throws IOException {
    var specification = ReferenceTestModel.constructMinimumSpecification();
    var rule = specification.getProvider().getRules().get(0);
    rule.setSelectivity(ruleSelectivity);
    SecretMatcher actualMatcher = SecretMatcher.build(rule, mockDurationStatistics(), SpecificationConfiguration.AUTO_TEST_FILE_DETECTION_DISABLED, true);
    var inputFile = inputFile(Path.of(".env"), "rule matching pattern", language, InputFile.Type.MAIN);
    var fileContext = inputFileContext(inputFile);

    var result = actualMatcher.findIn(fileContext);

    assertThat(result).hasSize(shouldRaise ? 1 : 0);
  }

  static Stream<Arguments> shouldRaiseIssueAccordingToSelectivityAndLanguageExistence() {
    return Stream.of(
      Arguments.of(Selectivity.SPECIFIC, null, true),
      Arguments.of(Selectivity.PROVIDER_GENERIC, null, true),
      Arguments.of(Selectivity.ANALYZER_GENERIC, null, true),

      Arguments.of(Selectivity.SPECIFIC, "myLanguage", true),
      Arguments.of(Selectivity.PROVIDER_GENERIC, "myLanguage", true),
      Arguments.of(Selectivity.ANALYZER_GENERIC, "myLanguage", false));
  }
}
