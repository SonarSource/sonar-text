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
package org.sonar.plugins.secrets.api.filters;

import java.util.Base64;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.sonar.plugins.secrets.configuration.deserialization.ReferenceTestModel;
import org.sonar.plugins.secrets.configuration.model.matching.Matching;
import org.sonar.plugins.secrets.configuration.model.matching.filter.DecodedBase64Module;
import org.sonar.plugins.secrets.configuration.model.matching.filter.HeuristicsFilter;
import org.sonar.plugins.secrets.configuration.model.matching.filter.StatisticalFilter;
import org.sonar.plugins.secrets.configuration.model.matching.filter.TopLevelPostModule;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

class PostFilterFactoryTest {

  private static Matching matching;

  @BeforeAll
  static void initialize() {
    matching = new Matching();
    matching.setPattern("\\b(?<groupName>candidate) secret\\b");
  }

  @Test
  void postFilterShouldReturnTrueOnHighEntropy() {
    var postModule = ReferenceTestModel.constructPostModule();

    PostFilter postFilter = PostFilterFactory.createFilter(postModule);

    assertThat(postFilter.apply("candidate secret with high entropy: lasdij2338f,.q29cm2acasd").passed()).isTrue();
  }

  @ParameterizedTest
  @ValueSource(strings = {
    "candidate secret with low entropy",
    "candidate secret with low entropy and patternNot:EXAMPLEKEY",
    "candidate secret with high entropy: lasdij2338f,.q29cm2acasd has patternNot:EXAMPLEKEY"
  })
  void postFilterShouldReturnFalse(String input) {
    var postModule = ReferenceTestModel.constructPostModule();

    PostFilter postFilter = PostFilterFactory.createFilter(postModule);

    assertThat(postFilter.apply(input).passed()).isFalse();
  }

  @Test
  void postFilterShouldReturnFalseOnLowEntropyWhenPatternNotIsNull() {
    var postModule = ReferenceTestModel.constructPostModule();
    postModule.setPatternNot(emptyList());

    PostFilter postFilter = PostFilterFactory.createFilter(postModule);

    assertThat(postFilter.apply("string with low entropy").passed()).isFalse();
  }

  @Test
  void postFilterShouldReturnTrueOnHighEntropyWhenPatternNotIsNull() {
    var postModule = ReferenceTestModel.constructPostModule();
    postModule.setPatternNot(emptyList());

    PostFilter postFilter = PostFilterFactory.createFilter(postModule);

    assertThat(postFilter.apply("rule matching EXAMPLEKEY pattern with high entropy: lasdij2338f,.q29cm2acasd").passed()).isTrue();
  }

  @Test
  void postFilterShouldReturnFalseOnLowEntropyWhenStatisticalFilterIsNull() {
    var postModule = ReferenceTestModel.constructPostModule();
    postModule.setStatisticalFilter(null);

    PostFilter postFilter = PostFilterFactory.createFilter(postModule);

    assertThat(postFilter.apply("rule matching EXAMPLEKEY pattern").passed()).isFalse();
  }

  @Test
  void postFilterShouldReturnTrueOnHighEntropyWhenStatisticalFilterIsNull() {
    var postModule = ReferenceTestModel.constructPostModule();
    postModule.setStatisticalFilter(null);

    PostFilter postFilter = PostFilterFactory.createFilter(postModule);

    assertThat(postFilter.apply("string with high entropy: lasdij2338f,.q29cm2acasd").passed()).isTrue();
  }

  @Test
  void statisticalFilterShouldReturnFalseOnLowEntropy() {
    var postModule = new TopLevelPostModule(null, null, emptyList(), statisticalFilterWithThreshold(4.2f), emptyList());

    PostFilter postFilter = PostFilterFactory.createFilter(postModule);

    assertThat(postFilter.apply("rule matching pattern").passed()).isFalse();
  }

  @Test
  void statisticalFilterShouldReturnTrueOnHighEntropy() {
    var postModule = new TopLevelPostModule(null, null, emptyList(), statisticalFilterWithThreshold(4.2f), emptyList());

    PostFilter postFilter = PostFilterFactory.createFilter(postModule);

    assertThat(postFilter.apply("string with high entropy: lasdij2338f,.q29cm2acasd").passed()).isTrue();
  }

  @Test
  void patternNotFilterShouldReturnTrue() {
    var postModule = new TopLevelPostModule(null, null, List.of("patternNot"), null, emptyList());

    PostFilter postFilter = PostFilterFactory.createFilter(postModule);

    assertThat(postFilter.apply("candidate secret").passed()).isTrue();
  }

  @Test
  void patternNotFilterShouldReturnFalse() {
    var postModule = new TopLevelPostModule(null, null, List.of("patternNot", "anythingElse"), null, emptyList());

    PostFilter postFilter = PostFilterFactory.createFilter(postModule);

    assertThat(postFilter.apply("candidate secret with patternNot").passed()).isFalse();
  }

  @Test
  void patternNotFilterShouldReturnFalseOnMultipleFoundPatternsProvided() {
    var postModule = new TopLevelPostModule(null, null, List.of("patternNot", "with"), null, emptyList());

    PostFilter postFilter = PostFilterFactory.createFilter(postModule);

    assertThat(postFilter.apply("candidate secret with patternNot").passed()).isFalse();
  }

  @Test
  void heuristicFilterShouldReturnTrue() {
    var postModule = new TopLevelPostModule(null, heuristicsFilterFor("uri"), emptyList(), null, emptyList());

    PostFilter postFilter = PostFilterFactory.createFilter(postModule);

    assertThat(postFilter.apply("not a valid uri").passed()).isTrue();
  }

  @Test
  void heuristicFilterShouldReturnFalse() {
    var postModule = new TopLevelPostModule(null, heuristicsFilterFor("uri"), emptyList(), null, emptyList());

    PostFilter postFilter = PostFilterFactory.createFilter(postModule);

    assertThat(postFilter.apply("https://sonarsource.com").passed()).isFalse();
  }

  private static StatisticalFilter statisticalFilterWithThreshold(float threshold) {
    var filter = new StatisticalFilter();
    filter.setThreshold(threshold);
    return filter;
  }

  private static HeuristicsFilter heuristicsFilterFor(String... heuristics) {
    var filter = new HeuristicsFilter();
    filter.setHeuristics(List.of(heuristics));
    return filter;
  }

  @ParameterizedTest
  @ValueSource(strings = {
    "candidate secret with low entropy",
    "candidate secret with low entropy and patternNot:EXAMPLEKEY",
    "candidate secret with high entropy: lasdij2338f,.q29cm2acasd has patternNot:EXAMPLEKEY",
    "candidate secret with high entropy: lasdij2338f,.q29cm2acasd"
  })
  void postFilterShouldEvaluateToTrueRegardlessOfInputWhenInputIsNull(String input) {
    PostFilter postFilter = PostFilterFactory.createFilter(null);
    assertThat(postFilter.apply(input).passed()).isTrue();
  }

  @ParameterizedTest
  @ValueSource(strings = {
    "candidate secret with low entropy",
    "candidate secret with low entropy and patternNot:EXAMPLEKEY",
    "candidate secret with high entropy: lasdij2338f,.q29cm2acasd has patternNot:EXAMPLEKEY",
    "candidate secret with high entropy: lasdij2338f,.q29cm2acasd"
  })
  void postFilterShouldAlwaysEvaluateToTrueRegardlessOfInputWhenStatFilterAndPatternNotIsNull(String input) {
    var postModule = ReferenceTestModel.constructPostModule();
    postModule.setStatisticalFilter(null);
    postModule.setPatternNot(emptyList());
    PostFilter postFilter = PostFilterFactory.createFilter(postModule);
    assertThat(postFilter.apply(input).passed()).isTrue();
  }

  @Test
  void shouldMatchOnBase64DecodedParts() {
    var postModule = new TopLevelPostModule(new DecodedBase64Module(List.of("\"alg\":"), emptyList(), DecodedBase64Module.Alphabet.DEFAULT), null, emptyList(), null, emptyList());
    var postFilter = PostFilterFactory.createFilter(postModule);

    assertThat(postFilter.apply("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9").passed()).isTrue();
  }

  @Test
  void shouldNotMatchOnMalformedBase64DecodedParts() {
    var postModule = new TopLevelPostModule(new DecodedBase64Module(List.of("\"alg\":"), emptyList(), DecodedBase64Module.Alphabet.DEFAULT), null, emptyList(), null, emptyList());
    var postFilter = PostFilterFactory.createFilter(postModule);

    assertThat(postFilter.apply("1248163264128").passed()).isFalse();
  }

  @ParameterizedTest
  @CsvSource(textBlock = """
    {"alg":"SHA256","info":"test secret"};true
    {"alg":"SHA256"};false
    {"key1":"value1","key2":"value2"};false
    "alg":"info":;true
    "alg"info";false
    """, delimiter = ';')
  void shouldMatchEachOnBase64DecodedParts(String input, boolean shouldMatch) {
    var postModule = new TopLevelPostModule(new DecodedBase64Module(List.of("\"alg\":", "\"info\":"), emptyList(), DecodedBase64Module.Alphabet.DEFAULT), null, emptyList(), null,
      emptyList());
    var postFilter = PostFilterFactory.createFilter(postModule);
    var encodedInput = Base64.getEncoder().encodeToString(input.getBytes());

    assertThat(postFilter.apply(encodedInput).passed()).isEqualTo(shouldMatch);
  }

  @Test
  void shouldMatchOnBase64DecodedPartsInY64Mode() {
    var postModule = new TopLevelPostModule(new DecodedBase64Module(List.of("&s=consumersecret&"), emptyList(), DecodedBase64Module.Alphabet.Y64), null, emptyList(), null,
      emptyList());
    var postFilter = PostFilterFactory.createFilter(postModule);

    assertThat(postFilter.apply("dj0yJmk9VXNwOWg3R3NvRDkyJmQ9WVdrOU4xTlFhM1JVTlRRbWNHbzlNQS0tJnM9Y29uc3VtZXJzZWNyZXQmeD0wYw--").passed()).isTrue();
  }

  @Test
  void shouldRejectOnMatchNotInBase64DecodedParts() {
    var postModule = new TopLevelPostModule(new DecodedBase64Module(emptyList(), List.of("\"alg\":"), DecodedBase64Module.Alphabet.DEFAULT), null, emptyList(), null, emptyList());
    var postFilter = PostFilterFactory.createFilter(postModule);

    // eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9 decodes to {"alg":"HS256","typ":"JWT"} which contains "alg":
    assertThat(postFilter.apply("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9").passed()).isFalse();
  }

  @Test
  void shouldAcceptWhenMatchNotPatternNotFound() {
    var postModule = new TopLevelPostModule(new DecodedBase64Module(emptyList(), List.of("\"notFound\":"), DecodedBase64Module.Alphabet.DEFAULT), null, emptyList(), null,
      emptyList());
    var postFilter = PostFilterFactory.createFilter(postModule);

    // eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9 decodes to {"alg":"HS256","typ":"JWT"} which doesn't contain "notFound":
    assertThat(postFilter.apply("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9").passed()).isTrue();
  }

  @ParameterizedTest
  @CsvSource(textBlock = """
    {"alg":"SHA256","info":"test"};true
    {"alg":"SHA256","info":"secret"};false
    {"alg":"SHA256"};false
    {"info":"test"};false
    {"key":"value"};false
    """, delimiter = ';')
  void shouldCombineMatchEachAndMatchNot(String input, boolean shouldMatch) {
    // matchEach: must contain "alg": AND "info":
    // matchNot: must NOT contain "secret"
    var postModule = new TopLevelPostModule(
      new DecodedBase64Module(List.of("\"alg\":", "\"info\":"), List.of("secret"), DecodedBase64Module.Alphabet.DEFAULT),
      null, emptyList(), null, emptyList());
    var postFilter = PostFilterFactory.createFilter(postModule);
    var encodedInput = Base64.getEncoder().encodeToString(input.getBytes());

    assertThat(postFilter.apply(encodedInput).passed()).isEqualTo(shouldMatch);
  }

  @Test
  void shouldRejectIfAnyMatchNotPatternMatches() {
    // matchNot with multiple patterns - should reject if ANY matches (OR logic)
    var postModule = new TopLevelPostModule(
      new DecodedBase64Module(emptyList(), List.of("\"alg\":", "\"typ\":"), DecodedBase64Module.Alphabet.DEFAULT),
      null, emptyList(), null, emptyList());
    var postFilter = PostFilterFactory.createFilter(postModule);

    // eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9 decodes to {"alg":"HS256","typ":"JWT"} which contains both
    assertThat(postFilter.apply("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9").passed()).isFalse();

    // Test with content that has only one of the matchNot patterns
    var inputWithOnlyAlg = Base64.getEncoder().encodeToString("{\"alg\":\"HS256\"}".getBytes());
    assertThat(postFilter.apply(inputWithOnlyAlg).passed()).isFalse();

    // Test with content that has none of the matchNot patterns
    var inputWithNeither = Base64.getEncoder().encodeToString("{\"key\":\"value\"}".getBytes());
    assertThat(postFilter.apply(inputWithNeither).passed()).isTrue();
  }

  @Test
  void whenEntropyFilterDisabledLowEntropySecretIsNotFiltered() {
    var postModule = ReferenceTestModel.constructPostModule();

    PostFilter filter = PostFilterFactory.createFilter(postModule, Set.of(SkippedFilter.ENTROPY_FILTER));
    FilterOutcome result = filter.apply("low entropy secret");

    assertThat(result.passed()).isTrue();
    assertThat(result.skipped()).contains(SkippedFilter.ENTROPY_FILTER);
  }

  @Test
  void whenEntropyFilterDisabledHighEntropySecretIsStillAccepted() {
    var postModule = ReferenceTestModel.constructPostModule();

    PostFilter filter = PostFilterFactory.createFilter(postModule, Set.of(SkippedFilter.ENTROPY_FILTER));
    FilterOutcome result = filter.apply("lasdij2338f,.q29cm2acasd high entropy");

    assertThat(result.passed()).isTrue();
    assertThat(result.skipped()).doesNotContain(SkippedFilter.ENTROPY_FILTER);
  }

  @Test
  void whenEntropyFilterDisabledPatternNotFilterIsStillApplied() {
    var postModule = ReferenceTestModel.constructPostModule();

    PostFilter filter = PostFilterFactory.createFilter(postModule, Set.of(SkippedFilter.ENTROPY_FILTER));
    FilterOutcome result = filter.apply("low entropy EXAMPLEKEY");

    assertThat(result.passed()).isFalse();
  }

  @Test
  void whenEntropyFilterEnabledBehaviorIsUnchanged() {
    var postModule = ReferenceTestModel.constructPostModule();

    PostFilter filter = PostFilterFactory.createFilter(postModule, Set.of());

    assertThat(filter.apply("low entropy secret").passed()).isFalse();
    assertThat(filter.apply("lasdij2338f,.q29cm2acasd high entropy").passed()).isTrue();
  }

  @Test
  void createFilterReturnsAcceptAllWhenPostModuleIsNull() {
    PostFilter filter = PostFilterFactory.createFilter(null, Set.of(SkippedFilter.ENTROPY_FILTER));
    FilterOutcome result = filter.apply("anything");

    assertThat(result.passed()).isTrue();
    assertThat(result.skipped()).doesNotContain(SkippedFilter.ENTROPY_FILTER);
  }

  @Test
  void whenEntropyFilterDisabledAndNoStatisticalFilterEntropySkippedIsFalse() {
    var postModule = ReferenceTestModel.constructPostModule();
    postModule.setStatisticalFilter(null);

    PostFilter filter = PostFilterFactory.createFilter(postModule, Set.of(SkippedFilter.ENTROPY_FILTER));
    FilterOutcome result = filter.apply("low entropy secret");

    assertThat(result.passed()).isTrue();
    assertThat(result.skipped()).doesNotContain(SkippedFilter.ENTROPY_FILTER);
  }
}
