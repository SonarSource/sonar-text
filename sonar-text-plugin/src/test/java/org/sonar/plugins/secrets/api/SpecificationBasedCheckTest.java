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
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.sonar.check.Rule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.sonar.plugins.common.TestUtils.analyze;
import static org.sonar.plugins.common.TestUtils.mockDurationStatistics;

class SpecificationBasedCheckTest {

  @Test
  void checkShouldRaiseIssueOnBasicDetection() throws IOException {
    String specificationLocation = "secretsConfiguration/";
    Set<String> specifications = Set.of("validMinSpec.sml");
    var specificationLoader = new SecretsSpecificationLoader(specificationLocation, specifications);

    String fileContent = "The content contains the rule matching pattern and various other characters.";
    ExampleCheck exampleCheck = new ExampleCheck();
    exampleCheck.initialize(specificationLoader, mockDurationStatistics(), SpecificationConfiguration.AUTO_TEST_FILE_DETECTION_ENABLED);

    assertThat(analyze(exampleCheck, fileContent)).containsExactly(
      "secrets:exampleKey [1:25-1:46] provider message");
  }

  @Test
  void checkShouldNotRaiseIssueWithPostFilterBecauseOfLowEntropy() throws IOException {
    String specificationLocation = "secretsConfiguration/";
    Set<String> specifications = Set.of("validReferenceSpec.sml");
    var specificationLoader = new SecretsSpecificationLoader(specificationLocation, specifications);

    String fileContent = "rule matching pattern with low entropy";
    ExampleCheck exampleCheck = new ExampleCheck();
    exampleCheck.initialize(specificationLoader, mockDurationStatistics(), SpecificationConfiguration.AUTO_TEST_FILE_DETECTION_ENABLED);

    assertThat(analyze(exampleCheck, fileContent)).isEmpty();
  }

  @Test
  void checkShouldRaiseIssueWhenFilterHasLowEntropyThreshold() throws IOException {
    String specificationLocation = "secretsConfiguration/postFilter/";
    Set<String> specifications = Set.of("postFilterSpec.sml");
    var specificationLoader = new SecretsSpecificationLoader(specificationLocation, specifications);
    specificationLoader.getRulesForKey("exampleKey").get(0).getDetection().getPost().getStatisticalFilter().setThreshold(3f);

    String fileContent = "rule matching pattern and post filter has low entropy";
    ExampleCheck exampleCheck = new ExampleCheck();
    exampleCheck.initialize(specificationLoader, mockDurationStatistics(), SpecificationConfiguration.AUTO_TEST_FILE_DETECTION_ENABLED);

    assertThat(analyze(exampleCheck, fileContent)).containsExactly(
      "secrets:exampleKey [1:0-1:21] rule message");
  }

  @Test
  void checkShouldNotRaiseIssueWithPostFilterBecauseOfPatternNot() throws IOException {
    String specificationLocation = "secretsConfiguration/postFilter/";
    Set<String> specifications = Set.of("postFilterSpec.sml");
    var specificationLoader = new SecretsSpecificationLoader(specificationLocation, specifications);
    specificationLoader.getRulesForKey("exampleKey").get(0).getDetection().getPost().setPatternNot(List.of("matching"));
    specificationLoader.getRulesForKey("exampleKey").get(0).getDetection().getPost().getStatisticalFilter().setThreshold(3f);

    String fileContent = "rule matching pattern and patternNot matching inside and post filter has low entropy";
    ExampleCheck exampleCheck = new ExampleCheck();
    exampleCheck.initialize(specificationLoader, mockDurationStatistics(), SpecificationConfiguration.AUTO_TEST_FILE_DETECTION_ENABLED);

    assertThat(analyze(exampleCheck, fileContent)).isEmpty();
  }

  @ParameterizedTest
  @MethodSource("contentPreFilterTestCases")
  void shouldSkipContentPreFiltersOnlyForCompatibleRules(
    String specFile,
    boolean shouldExecuteContentPreFilters) {
    try (var mockedStatic = Mockito.mockStatic(SecretMatcher.class)) {
      var specificationLocation = "secretsConfiguration/pre-filter/";
      var specifications = Set.of(specFile);
      var specificationLoader = new SecretsSpecificationLoader(specificationLocation, specifications);

      new ExampleCheck().initialize(specificationLoader, mockDurationStatistics(), SpecificationConfiguration.AUTO_TEST_FILE_DETECTION_ENABLED);

      mockedStatic.verify(() -> SecretMatcher.build(any(), any(), any(), eq(shouldExecuteContentPreFilters)), atLeastOnce());
    }
  }

  static Stream<Arguments> contentPreFilterTestCases() {
    return Stream.of(
      Arguments.of("singleOptimizableRule.sml", false),
      // Due to the current limitations of the implementation, we cannot optimize content pre-filters for multiple rules. See SONARTEXT-585.
      Arguments.of("multipleOptimizableRules.sml", true),
      Arguments.of("singleNonOptimizableRule.sml", true),
      Arguments.of("multipleNonOptimizableRules.sml", true));
  }

  @Rule(key = "exampleKey")
  static class ExampleCheck extends SpecificationBasedCheck {

  }

}
