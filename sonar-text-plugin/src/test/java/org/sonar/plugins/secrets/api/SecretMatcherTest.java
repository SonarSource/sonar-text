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
import java.nio.file.Path;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.plugins.secrets.configuration.deserialization.ReferenceTestModel;
import org.sonar.plugins.secrets.configuration.model.Rule;
import org.sonar.plugins.secrets.configuration.model.RuleScope;
import org.sonar.plugins.secrets.configuration.model.matching.filter.PreModule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.plugins.common.TestUtils.inputFile;
import static org.sonar.plugins.common.TestUtils.inputFileContext;
import static org.sonar.plugins.secrets.api.AuxiliaryPatternMatcherFactoryTest.constructReferenceAuxiliaryMatcher;
import static org.sonar.plugins.secrets.api.SecretMatcherAssert.assertThat;
import static org.sonar.plugins.secrets.configuration.model.RuleScope.MAIN;
import static org.sonar.plugins.secrets.configuration.model.RuleScope.TEST;
import static org.sonar.plugins.secrets.utils.TestUtils.mockDurationStatistics;

class SecretMatcherTest {

  @Test
  void testConstructionOfSimpleDetection() {
    Rule rule = ReferenceTestModel.constructMinimumSpecification().getProvider().getRules().get(0);

    PatternMatcher patternMatcher = new PatternMatcher("\\b(rule matching pattern)\\b");
    SecretMatcher expectedMatcher = new SecretMatcher(
      rule.getId(),
      rule.getMetadata().getMessage(),
      patternMatcher,
      AuxiliaryPatternMatcher.NO_FILTERING_AUXILIARY_MATCHER,
      file -> true,
      s -> true,
      mockDurationStatistics());

    SecretMatcher actualMatcher = SecretMatcher.build(rule, mockDurationStatistics());

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
    SecretMatcher expectedMatcher = new SecretMatcher(
      rule.getId(),
      rule.getMetadata().getMessage(),
      patternMatcher,
      constructReferenceAuxiliaryMatcher(),
      file -> true,
      expectedPredicate,
      mockDurationStatistics());

    SecretMatcher actualMatcher = SecretMatcher.build(rule, mockDurationStatistics());

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
      patternMatcher,
      AuxiliaryPatternMatcher.NO_FILTERING_AUXILIARY_MATCHER,
      file -> true,
      s -> true,
      mockDurationStatistics());

    SecretMatcher actualMatcher = SecretMatcher.build(rule, mockDurationStatistics());

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
    SecretMatcher actualMatcher = SecretMatcher.build(rule, mockDurationStatistics());
    var inputFile = inputFile(Path.of(".env"), "rule matching pattern", null, type);
    var fileContext = inputFileContext(inputFile);

    var result = actualMatcher.findIn(fileContext);

    assertThat(result).hasSize(expectedMatches);
  }
}
