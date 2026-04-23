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
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import org.sonar.plugins.secrets.api.filters.FilterOutcome;
import org.sonar.plugins.secrets.api.filters.PostFilter;
import org.sonar.plugins.secrets.api.filters.SkippedFilter;
import org.sonar.plugins.secrets.configuration.deserialization.ReferenceTestModel;
import org.sonar.plugins.secrets.configuration.model.Rule;
import org.sonar.plugins.secrets.configuration.model.Selectivity;
import org.sonar.plugins.secrets.configuration.model.matching.filter.NamedPostModule;
import org.sonar.plugins.secrets.configuration.model.matching.filter.StatisticalFilter;
import org.sonar.plugins.secrets.configuration.model.matching.filter.TopLevelPostModule;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.plugins.common.TestUtils.inputFile;
import static org.sonar.plugins.common.TestUtils.inputFileContext;
import static org.sonar.plugins.common.TestUtils.mockDurationStatistics;
import static org.sonar.plugins.secrets.api.AuxiliaryPatternMatcherFactoryTest.constructReferenceAuxiliaryMatcher;
import static org.sonar.plugins.secrets.api.SecretMatcherAssert.assertThat;
import static org.sonar.plugins.secrets.api.filters.FilterOutcome.ACCEPTED;

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
      PostFilter.ACCEPT_ALL,
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
    final Predicate<String> finalExpectedPredicate = expectedPredicate;
    var expectedGroupFilter = (PostFilter) candidateSecret -> {
      String base64Decoded = null;
      try {
        base64Decoded = new String(Base64.decode(candidateSecret), StandardCharsets.UTF_8);
      } catch (Exception e) {
      }
      boolean passed = !Heuristics.matchesHeuristics(candidateSecret, List.of("uri")) && base64Decoded != null && base64Decoded.equals("\"alg\":");
      return passed ? FilterOutcome.ACCEPTED : FilterOutcome.REJECTED;
    };
    PostFilter expectedPostFilter = candidateSecret -> finalExpectedPredicate.test(candidateSecret) ? FilterOutcome.ACCEPTED : FilterOutcome.REJECTED;
    SecretMatcher expectedMatcher = new SecretMatcher(
      rule.getId(),
      rule.getMetadata().getMessage(),
      rule.getSelectivity(),
      patternMatcher,
      constructReferenceAuxiliaryMatcher(),
      file -> true,
      expectedPostFilter,
      Map.of("groupName", expectedGroupFilter),
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
      PostFilter.ACCEPT_ALL,
      emptyMap(),
      mockDurationStatistics());

    SecretMatcher actualMatcher = SecretMatcher.build(rule, mockDurationStatistics(), SpecificationConfiguration.AUTO_TEST_FILE_DETECTION_ENABLED, true);

    assertThat(actualMatcher).behavesLike(expectedMatcher);
  }

  static List<Arguments> shouldFindIssueInMainFile() {
    return List.of(
      Arguments.of(InputFile.Type.MAIN, true),
      Arguments.of(InputFile.Type.TEST, false));
  }

  @ParameterizedTest
  @MethodSource
  void shouldFindIssueInMainFile(InputFile.Type type, boolean shouldRaise) throws IOException {
    var specification = ReferenceTestModel.constructMinimumSpecification();
    var rule = specification.getProvider().getRules().get(0);
    SecretMatcher actualMatcher = SecretMatcher.build(rule, mockDurationStatistics(), SpecificationConfiguration.AUTO_TEST_FILE_DETECTION_ENABLED, true);
    var inputFile = inputFile(Path.of(".env"), "rule matching pattern", null, type);
    var fileContext = inputFileContext(inputFile);

    var result = actualMatcher.findIn(fileContext);

    assertThat(result).hasSize(shouldRaise ? 1 : 0);
  }

  @Test
  void getMessageForCandidateReturnsSameMessageWhenEntropyFilterEnabled() {
    Rule rule = ReferenceTestModel.constructReferenceSpecification().getProvider().getRules().get(0);
    SecretMatcher matcher = SecretMatcher.build(rule, mockDurationStatistics(), SpecificationConfiguration.AUTO_TEST_FILE_DETECTION_ENABLED, true);

    var match = new CandidateMatch("AAAAAAAAAAAAAAAA", 0, 16, Map.of());
    var acceptedMatch = new MatchResult(match, ACCEPTED);
    assertThat(matcher.getMessageForCandidate(acceptedMatch.outcome())).isEqualTo(rule.getMetadata().getMessage());
  }

  @Test
  void getMessageForCandidateAppendsLabelForLowEntropyWhenEntropyFilterDisabled() {
    Rule rule = ReferenceTestModel.constructReferenceSpecification().getProvider().getRules().get(0);
    SecretMatcher matcher = SecretMatcher.build(rule, mockDurationStatistics(), SpecificationConfiguration.AUTO_TEST_FILE_DETECTION_ENABLED, true);

    var match = new CandidateMatch("AAAAAAAAAAAAAAAA", 0, 16, Map.of());
    var acceptedMatch = new MatchResult(match, FilterOutcome.passedWithSkipped(SkippedFilter.ENTROPY_FILTER));
    assertThat(matcher.getMessageForCandidate(acceptedMatch.outcome())).isEqualTo(rule.getMetadata().getMessage() + " (low-confidence match, entropy filter is disabled)");
  }

  @Test
  void getMessageForCandidateDoesNotAppendLabelForHighEntropyWhenEntropyFilterDisabled() {
    Rule rule = ReferenceTestModel.constructReferenceSpecification().getProvider().getRules().get(0);
    SecretMatcher matcher = SecretMatcher.build(rule, mockDurationStatistics(), SpecificationConfiguration.AUTO_TEST_FILE_DETECTION_ENABLED, true);

    var match = new CandidateMatch("LGYIh8rDziCXCgDCUbJq1h7CKwNqnpA1il4MXL+y", 0, 41, Map.of());
    var acceptedMatch = new MatchResult(match, FilterOutcome.ACCEPTED);
    assertThat(matcher.getMessageForCandidate(acceptedMatch.outcome())).isEqualTo(rule.getMetadata().getMessage());
  }

  @Test
  void getMessageForCandidateAppendsLabelForGroupLevelEntropyWhenEntropyFilterDisabled() {
    Rule rule = ruleWithGroupStatisticalFilter();
    SecretMatcher matcher = SecretMatcher.build(rule, mockDurationStatistics(), SpecificationConfiguration.AUTO_TEST_FILE_DETECTION_ENABLED, true);

    var groupMatch = new CandidateMatch("AAAAAAAAAAAAAAAA", 0, 16, Map.of());
    var match = new CandidateMatch("AAAAAAAAAAAAAAAA", 0, 16, Map.of("secret", groupMatch));
    var acceptedMatch = new MatchResult(match, FilterOutcome.passedWithSkipped(SkippedFilter.ENTROPY_FILTER));
    assertThat(matcher.getMessageForCandidate(acceptedMatch.outcome())).isEqualTo(rule.getMetadata().getMessage() + " (low-confidence match, entropy filter is disabled)");
  }

  @Test
  void getMessageForCandidateAppendsLabelWhenGroupTextHasLowEntropyButFullMatchHasHighEntropy() {
    Rule rule = ruleWithGroupStatisticalFilter();
    SecretMatcher matcher = SecretMatcher.build(rule, mockDurationStatistics(), SpecificationConfiguration.AUTO_TEST_FILE_DETECTION_ENABLED, true);

    var groupMatch = new CandidateMatch("AAAAAAAAAAAAAAAA", 41, 57, Map.of());
    var match = new CandidateMatch("LGYIh8rDziCXCgDCUbJq1h7CKwNqnpA1il4MXL+y", 0, 41, Map.of("secret", groupMatch));
    // Guard: the full match has high entropy but the group capture "AAAAAAAAAAAAAAAA" has entropy 0,
    // below the threshold of 4.2, so the low-confidence suffix must be appended via the group-level check.
    var acceptedMatch = new MatchResult(match, FilterOutcome.passedWithSkipped(SkippedFilter.ENTROPY_FILTER));
    assertThat(matcher.getMessageForCandidate(acceptedMatch.outcome()))
      .isEqualTo(rule.getMetadata().getMessage() + " (low-confidence match, entropy filter is disabled)");
  }

  @Test
  void getMessageForCandidateReturnsSameMessageWhenRuleHasNoStatisticalFilter() {
    Rule rule = ReferenceTestModel.constructMinimumSpecification().getProvider().getRules().get(0);
    SecretMatcher matcher = SecretMatcher.build(rule, mockDurationStatistics(), SpecificationConfiguration.AUTO_TEST_FILE_DETECTION_ENABLED, true);

    var match = new CandidateMatch("AAAAAAAAAAAAAAAA", 0, 16, Map.of());
    var acceptedMatch = new MatchResult(match, ACCEPTED);
    assertThat(matcher.getMessageForCandidate(acceptedMatch.outcome())).isEqualTo(rule.getMetadata().getMessage());
  }

  @Test
  void findWithResultsReportsEntropySkippedForLowEntropyWhenDisabled() throws IOException {
    var rule = ruleWithStatisticalFilterOnly();
    var configWithEntropyDisabled = new SpecificationConfiguration(false, Set.of(SkippedFilter.ENTROPY_FILTER), MessageFormatter.RULE_MESSAGE);
    SecretMatcher matcher = SecretMatcher.build(rule, mockDurationStatistics(), configWithEntropyDisabled, true);

    var inputFile = inputFile(Path.of(".env"), "rule matching pattern", null, InputFile.Type.MAIN);
    var fileContext = inputFileContext(inputFile);

    var results = matcher.findIn(fileContext);
    assertThat(results).hasSize(1);
    assertThat(results.get(0).outcome().passed()).isTrue();
    assertThat(results.get(0).outcome().skipped()).contains(SkippedFilter.ENTROPY_FILTER);
  }

  @Test
  void findWithResultsReportsEntropySkippedForGroupLevelWhenDisabled() throws IOException {
    var rule = ruleWithGroupStatisticalFilter();
    var configWithEntropyDisabled = new SpecificationConfiguration(false, Set.of(SkippedFilter.ENTROPY_FILTER), MessageFormatter.RULE_MESSAGE);
    SecretMatcher matcher = SecretMatcher.build(rule, mockDurationStatistics(), configWithEntropyDisabled, true);

    var inputFile = inputFile(Path.of(".env"), "rule matching pattern", null, InputFile.Type.MAIN);
    var fileContext = inputFileContext(inputFile);

    var results = matcher.findIn(fileContext);
    assertThat(results).hasSize(1);
    assertThat(results.get(0).outcome().passed()).isTrue();
    assertThat(results.get(0).outcome().skipped()).contains(SkippedFilter.ENTROPY_FILTER);
  }

  @Test
  void findInReturnsLowEntropyMatchesWhenEntropyFilterDisabled() throws IOException {
    var rule = ruleWithStatisticalFilterOnly();
    var configWithEntropyDisabled = new SpecificationConfiguration(false, Set.of(SkippedFilter.ENTROPY_FILTER), MessageFormatter.RULE_MESSAGE);
    SecretMatcher matcher = SecretMatcher.build(rule, mockDurationStatistics(), configWithEntropyDisabled, true);

    // "rule matching pattern" has low entropy but should still be found when filter is disabled
    var inputFile = inputFile(Path.of(".env"), "rule matching pattern", null, InputFile.Type.MAIN);
    var fileContext = inputFileContext(inputFile);

    var result = matcher.findIn(fileContext);
    assertThat(result).hasSize(1);
  }

  @Test
  void findInFiltersOutLowEntropyMatchesWhenEntropyFilterEnabled() throws IOException {
    var rule = ruleWithStatisticalFilterOnly();
    SecretMatcher matcher = SecretMatcher.build(rule, mockDurationStatistics(), SpecificationConfiguration.AUTO_TEST_FILE_DETECTION_DISABLED, true);

    // "rule matching pattern" has low entropy and should be filtered out
    var inputFile = inputFile(Path.of(".env"), "rule matching pattern", null, InputFile.Type.MAIN);
    var fileContext = inputFileContext(inputFile);

    var result = matcher.findIn(fileContext);
    assertThat(result).isEmpty();
  }

  @Test
  void findInReturnsLowEntropyGroupMatchesWhenEntropyFilterDisabled() throws IOException {
    var rule = ruleWithGroupStatisticalFilter();
    var configWithEntropyDisabled = new SpecificationConfiguration(false, Set.of(SkippedFilter.ENTROPY_FILTER), MessageFormatter.RULE_MESSAGE);
    SecretMatcher matcher = SecretMatcher.build(rule, mockDurationStatistics(), configWithEntropyDisabled, true);

    // "rule matching pattern" has low entropy but the group predicate should not filter it out when entropy filter is disabled
    var inputFile = inputFile(Path.of(".env"), "rule matching pattern", null, InputFile.Type.MAIN);
    var fileContext = inputFileContext(inputFile);

    var result = matcher.findIn(fileContext);
    assertThat(result).hasSize(1);
  }

  @Test
  void findInFiltersOutLowEntropyGroupMatchesWhenEntropyFilterEnabled() throws IOException {
    var rule = ruleWithGroupStatisticalFilter();
    SecretMatcher matcher = SecretMatcher.build(rule, mockDurationStatistics(), SpecificationConfiguration.AUTO_TEST_FILE_DETECTION_DISABLED, true);

    // "rule matching pattern" has low entropy and should be filtered out by the group predicate
    var inputFile = inputFile(Path.of(".env"), "rule matching pattern", null, InputFile.Type.MAIN);
    var fileContext = inputFileContext(inputFile);

    var result = matcher.findIn(fileContext);
    assertThat(result).isEmpty();
  }

  private static Rule ruleWithStatisticalFilterOnly() {
    var spec = ReferenceTestModel.constructMinimumSpecification();
    var rule = spec.getProvider().getRules().get(0);
    var statisticalFilter = new StatisticalFilter();
    statisticalFilter.setThreshold(4.2f);
    var postModule = new TopLevelPostModule(null, null, Collections.emptyList(), statisticalFilter, Collections.emptyList());
    rule.getDetection().setPost(postModule);
    return rule;
  }

  private static Rule ruleWithGroupStatisticalFilter() {
    var spec = ReferenceTestModel.constructMinimumSpecification();
    var rule = spec.getProvider().getRules().get(0);
    rule.getDetection().getMatching().setPattern("(?<secret>rule matching pattern)");
    var groupStatisticalFilter = new StatisticalFilter();
    groupStatisticalFilter.setThreshold(4.2f);
    var namedGroup = new NamedPostModule("secret", null, null, Collections.emptyList(), groupStatisticalFilter);
    var postModule = new TopLevelPostModule(null, null, Collections.emptyList(), null, List.of(namedGroup));
    rule.getDetection().setPost(postModule);
    return rule;
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
