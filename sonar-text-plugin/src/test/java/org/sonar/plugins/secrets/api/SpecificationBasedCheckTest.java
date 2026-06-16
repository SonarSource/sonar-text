/*
 * SonarQube Text Plugin
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.check.Rule;
import org.sonar.plugins.secrets.api.filters.RejectionLogger;
import org.sonar.plugins.secrets.api.filters.SkippedFilter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.sonar.plugins.common.TestUtils.analyze;
import static org.sonar.plugins.common.TestUtils.inputFile;
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

  @Test
  void checkShouldRaiseFakeSecretIssueWhenEntropyFilterDisabledAndSecretHasLowEntropy() throws IOException {
    var configWithEntropyDisabled = new SpecificationConfiguration(true, Set.of(SkippedFilter.ENTROPY_FILTER), MessageFormatter.RULE_MESSAGE, RejectionLogger.DISABLED);
    var exampleCheck = initializeExampleCheck("secretsConfiguration/postFilter/", "postFilterSpec.sml", configWithEntropyDisabled);

    assertThat(analyze(exampleCheck, "rule matching pattern")).containsExactly(
      "secrets:exampleKey [1:0-1:21] rule message (low-confidence match, disabled filters: entropy)");
  }

  @Test
  void checkShouldNotRaiseFakeSecretIssueWhenEntropyFilterDisabledAndSecretHasHighEntropy() throws IOException {
    var specificationLoader = new SecretsSpecificationLoader("secretsConfiguration/postFilter/", Set.of("postFilterSpec.sml"));
    // Lower the threshold to 3f so that "rule matching pattern" (entropy ~3.76) passes as high entropy
    specificationLoader.getRulesForKey("exampleKey").get(0).getDetection().getPost().getStatisticalFilter().setThreshold(3f);
    var configWithEntropyDisabled = new SpecificationConfiguration(true, Set.of(SkippedFilter.ENTROPY_FILTER), MessageFormatter.RULE_MESSAGE, RejectionLogger.DISABLED);
    var exampleCheck = initializeExampleCheck(specificationLoader, configWithEntropyDisabled);

    assertThat(analyze(exampleCheck, "rule matching pattern")).containsExactly(
      "secrets:exampleKey [1:0-1:21] rule message");
  }

  @Test
  void checkShouldRaiseLowConfidenceIssueWhenTestFilesFilterDisabledAndFileIsAutomaticallyDetectedTestFile() throws IOException {
    var config = new SpecificationConfiguration(true, Set.of(SkippedFilter.TEST_FILES_FILTER), MessageFormatter.RULE_MESSAGE, RejectionLogger.DISABLED);
    var exampleCheck = initializeExampleCheck("secretsConfiguration/", "validMinSpec.sml", config);

    String fileContent = "The content contains the rule matching pattern and various other characters.";
    var testFile = inputFile(Path.of("test.env"), fileContent, null, InputFile.Type.MAIN);

    assertThat(analyze(exampleCheck, testFile)).containsExactly(
      "secrets:exampleKey [1:25-1:46] provider message (low-confidence match, disabled filters: automatic test file detection)");
  }

  @Test
  void checkShouldNotRaiseIssueInAutomaticTestFileByDefault() throws IOException {
    var exampleCheck = initializeExampleCheck("secretsConfiguration/", "validMinSpec.sml", SpecificationConfiguration.AUTO_TEST_FILE_DETECTION_ENABLED);

    String fileContent = "The content contains the rule matching pattern and various other characters.";
    var testFile = inputFile(Path.of("test.env"), fileContent, null, InputFile.Type.MAIN);

    assertThat(analyze(exampleCheck, testFile)).isEmpty();
  }

  @Test
  void checkShouldRaiseNormalIssueInNonTestFileEvenWhenTestFilesFilterDisabled() throws IOException {
    var config = new SpecificationConfiguration(true, Set.of(SkippedFilter.TEST_FILES_FILTER), MessageFormatter.RULE_MESSAGE, RejectionLogger.DISABLED);
    var exampleCheck = initializeExampleCheck("secretsConfiguration/", "validMinSpec.sml", config);

    String fileContent = "The content contains the rule matching pattern and various other characters.";
    assertThat(analyze(exampleCheck, fileContent)).containsExactly(
      "secrets:exampleKey [1:25-1:46] provider message");
  }

  @Test
  void checkShouldRaiseLowConfidenceIssueWhenKnownFakeSecretFilterDisabledAndCandidateMatchesPatternNot() throws IOException {
    var specificationLoader = new SecretsSpecificationLoader("secretsConfiguration/postFilter/", Set.of("postFilterSpec.sml"));
    specificationLoader.getRulesForKey("exampleKey").get(0).getDetection().getPost().setPatternNot(List.of("matching"));
    // Lower the threshold so the candidate passes the entropy filter and rejection comes only from patternNot
    specificationLoader.getRulesForKey("exampleKey").get(0).getDetection().getPost().getStatisticalFilter().setThreshold(3f);
    var configWithKnownFakeSecretDisabled = new SpecificationConfiguration(true, Set.of(SkippedFilter.KNOWN_FAKE_SECRET_FILTER), MessageFormatter.RULE_MESSAGE,
      RejectionLogger.DISABLED);
    var exampleCheck = initializeExampleCheck(specificationLoader, configWithKnownFakeSecretDisabled);

    assertThat(analyze(exampleCheck, "rule matching pattern")).containsExactly(
      "secrets:exampleKey [1:0-1:21] rule message (low-confidence match, disabled filters: known fake secrets)");
  }

  @Test
  void checkShouldNotRaiseIssueWhenKnownFakeSecretFilterDisabledAndCandidateDoesNotMatchPatternNot() throws IOException {
    var specificationLoader = new SecretsSpecificationLoader("secretsConfiguration/postFilter/", Set.of("postFilterSpec.sml"));
    specificationLoader.getRulesForKey("exampleKey").get(0).getDetection().getPost().setPatternNot(List.of("nonMatchingWord"));
    var configWithKnownFakeSecretDisabled = new SpecificationConfiguration(true, Set.of(SkippedFilter.KNOWN_FAKE_SECRET_FILTER), MessageFormatter.RULE_MESSAGE,
      RejectionLogger.DISABLED);
    var exampleCheck = initializeExampleCheck(specificationLoader, configWithKnownFakeSecretDisabled);

    // Low entropy candidate is still filtered out by the (still-enabled) entropy filter; we just don't add the suffix.
    assertThat(analyze(exampleCheck, "rule matching pattern")).isEmpty();
  }

  private static ExampleCheck initializeExampleCheck(String specificationLocation, String specificationFile, SpecificationConfiguration config) {
    return initializeExampleCheck(new SecretsSpecificationLoader(specificationLocation, Set.of(specificationFile)), config);
  }

  private static ExampleCheck initializeExampleCheck(SecretsSpecificationLoader specificationLoader, SpecificationConfiguration config) {
    var exampleCheck = new ExampleCheck();
    exampleCheck.initialize(specificationLoader, mockDurationStatistics(), config);
    return exampleCheck;
  }

  @Rule(key = "exampleKey")
  static class ExampleCheck extends SpecificationBasedCheck {

  }

}
