/*
 * SonarQube Text Plugin
 * Copyright (C) 2021-2025 SonarSource Sàrl
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

import java.util.Base64;
import java.util.List;
import java.util.function.Predicate;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.sonar.plugins.secrets.configuration.deserialization.ReferenceTestModel;
import org.sonar.plugins.secrets.configuration.model.matching.Matching;
import org.sonar.plugins.secrets.configuration.model.matching.filter.DecodedBase64Module;
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

    Predicate<String> postFilter = PostFilterFactory.createPredicate(postModule);

    assertThat(postFilter.test("candidate secret with high entropy: lasdij2338f,.q29cm2acasd")).isTrue();
  }

  @ParameterizedTest
  @ValueSource(strings = {
    "candidate secret with low entropy",
    "candidate secret with low entropy and patternNot:EXAMPLEKEY",
    "candidate secret with high entropy: lasdij2338f,.q29cm2acasd has patternNot:EXAMPLEKEY"
  })
  void postFilterShouldReturnFalse(String input) {
    var postModule = ReferenceTestModel.constructPostModule();

    Predicate<String> postFilter = PostFilterFactory.createPredicate(postModule);

    assertThat(postFilter.test(input)).isFalse();
  }

  @Test
  void postFilterShouldReturnFalseOnLowEntropyWhenPatternNotIsNull() {
    var postModule = ReferenceTestModel.constructPostModule();
    postModule.setPatternNot(emptyList());

    Predicate<String> postFilter = PostFilterFactory.createPredicate(postModule);

    assertThat(postFilter.test("string with low entropy")).isFalse();
  }

  @Test
  void postFilterShouldReturnTrueOnHighEntropyWhenPatternNotIsNull() {
    var postModule = ReferenceTestModel.constructPostModule();
    postModule.setPatternNot(emptyList());

    Predicate<String> postFilter = PostFilterFactory.createPredicate(postModule);

    assertThat(postFilter.test("rule matching EXAMPLEKEY pattern with high entropy: lasdij2338f,.q29cm2acasd")).isTrue();
  }

  @Test
  void postFilterShouldReturnFalseOnLowEntropyWhenStatisticalFilterIsNull() {
    var postModule = ReferenceTestModel.constructPostModule();
    postModule.setStatisticalFilter(null);

    Predicate<String> postFilter = PostFilterFactory.createPredicate(postModule);

    assertThat(postFilter.test("rule matching EXAMPLEKEY pattern")).isFalse();
  }

  @Test
  void postFilterShouldReturnTrueOnHighEntropyWhenStatisticalFilterIsNull() {
    var postModule = ReferenceTestModel.constructPostModule();
    postModule.setStatisticalFilter(null);

    Predicate<String> postFilter = PostFilterFactory.createPredicate(postModule);

    assertThat(postFilter.test("string with high entropy: lasdij2338f,.q29cm2acasd")).isTrue();
  }

  @Test
  void statisticalFilterShouldReturnFalseOnLowEntropy() {
    var postModule = ReferenceTestModel.constructPostModule();

    Predicate<String> predicate = PostFilterFactory.filterForStatisticalFilter(postModule.getStatisticalFilter());

    assertThat(predicate.test("rule matching pattern")).isFalse();
  }

  @Test
  void statisticalFilterShouldReturnTrueOnHighEntropy() {
    var postModule = ReferenceTestModel.constructPostModule();

    Predicate<String> predicate = PostFilterFactory.filterForStatisticalFilter(postModule.getStatisticalFilter());

    assertThat(predicate.test("string with high entropy: lasdij2338f,.q29cm2acasd")).isTrue();
  }

  @Test
  void patternNotFilterShouldReturnTrue() {
    Predicate<String> predicate = PostFilterFactory.filterForPatternNot(List.of("patternNot"));

    assertThat(predicate.test("candidate secret")).isTrue();
  }

  @Test
  void patternNotFilterShouldReturnFalse() {
    Predicate<String> predicate = PostFilterFactory.filterForPatternNot(List.of("patternNot", "anythingElse"));

    assertThat(predicate.test("candidate secret with patternNot")).isFalse();
  }

  @Test
  void patternNotFilterShouldReturnFalseOnMultipleFoundPatternsProvided() {
    Predicate<String> predicate = PostFilterFactory.filterForPatternNot(List.of("patternNot", "with"));

    assertThat(predicate.test("candidate secret with patternNot")).isFalse();
  }

  @Test
  void heuristicFilterShouldReturnTrue() {
    var postModule = ReferenceTestModel.constructPostModule();

    Predicate<String> predicate = PostFilterFactory.filterForHeuristicsFilter(postModule.getGroups().get(0).getHeuristicFilter());

    assertThat(predicate.test("not a valid uri")).isTrue();
  }

  @Test
  void heuristicFilterShouldReturnFalse() {
    var postModule = ReferenceTestModel.constructPostModule();

    Predicate<String> predicate = PostFilterFactory.filterForHeuristicsFilter(postModule.getGroups().get(0).getHeuristicFilter());

    assertThat(predicate.test("https://sonarsource.com")).isFalse();
  }

  @ParameterizedTest
  @ValueSource(strings = {
    "candidate secret with low entropy",
    "candidate secret with low entropy and patternNot:EXAMPLEKEY",
    "candidate secret with high entropy: lasdij2338f,.q29cm2acasd has patternNot:EXAMPLEKEY",
    "candidate secret with high entropy: lasdij2338f,.q29cm2acasd"
  })
  void postFilterShouldEvaluateToTrueRegardlessOfInputWhenInputIsNull(String input) {
    Predicate<String> postFilter = PostFilterFactory.createPredicate(null);
    assertThat(postFilter.test(input)).isTrue();
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
    Predicate<String> postFilter = PostFilterFactory.createPredicate(postModule);
    assertThat(postFilter.test(input)).isTrue();
  }

  @Test
  void shouldMatchOnBase64DecodedParts() {
    var postModule = new TopLevelPostModule(new DecodedBase64Module(List.of("\"alg\":"), emptyList(), DecodedBase64Module.Alphabet.DEFAULT), null, emptyList(), null, emptyList());
    var postFilter = PostFilterFactory.createPredicate(postModule);

    assertThat(postFilter.test("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9")).isTrue();
  }

  @Test
  void shouldNotMatchOnMalformedBase64DecodedParts() {
    var postModule = new TopLevelPostModule(new DecodedBase64Module(List.of("\"alg\":"), emptyList(), DecodedBase64Module.Alphabet.DEFAULT), null, emptyList(), null, emptyList());
    var postFilter = PostFilterFactory.createPredicate(postModule);

    assertThat(postFilter.test("1248163264128")).isFalse();
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
    var postFilter = PostFilterFactory.createPredicate(postModule);
    var encodedInput = Base64.getEncoder().encodeToString(input.getBytes());

    assertThat(postFilter.test(encodedInput)).isEqualTo(shouldMatch);
  }

  @Test
  void shouldMatchOnBase64DecodedPartsInY64Mode() {
    var postModule = new TopLevelPostModule(new DecodedBase64Module(List.of("&s=consumersecret&"), emptyList(), DecodedBase64Module.Alphabet.Y64), null, emptyList(), null,
      emptyList());
    var postFilter = PostFilterFactory.createPredicate(postModule);

    assertThat(postFilter.test("dj0yJmk9VXNwOWg3R3NvRDkyJmQ9WVdrOU4xTlFhM1JVTlRRbWNHbzlNQS0tJnM9Y29uc3VtZXJzZWNyZXQmeD0wYw--")).isTrue();
  }

  @Test
  void shouldRejectOnMatchNotInBase64DecodedParts() {
    var postModule = new TopLevelPostModule(new DecodedBase64Module(emptyList(), List.of("\"alg\":"), DecodedBase64Module.Alphabet.DEFAULT), null, emptyList(), null, emptyList());
    var postFilter = PostFilterFactory.createPredicate(postModule);

    // eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9 decodes to {"alg":"HS256","typ":"JWT"} which contains "alg":
    assertThat(postFilter.test("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9")).isFalse();
  }

  @Test
  void shouldAcceptWhenMatchNotPatternNotFound() {
    var postModule = new TopLevelPostModule(new DecodedBase64Module(emptyList(), List.of("\"notFound\":"), DecodedBase64Module.Alphabet.DEFAULT), null, emptyList(), null,
      emptyList());
    var postFilter = PostFilterFactory.createPredicate(postModule);

    // eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9 decodes to {"alg":"HS256","typ":"JWT"} which doesn't contain "notFound":
    assertThat(postFilter.test("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9")).isTrue();
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
    var postFilter = PostFilterFactory.createPredicate(postModule);
    var encodedInput = Base64.getEncoder().encodeToString(input.getBytes());

    assertThat(postFilter.test(encodedInput)).isEqualTo(shouldMatch);
  }

  @Test
  void shouldRejectIfAnyMatchNotPatternMatches() {
    // matchNot with multiple patterns - should reject if ANY matches (OR logic)
    var postModule = new TopLevelPostModule(
      new DecodedBase64Module(emptyList(), List.of("\"alg\":", "\"typ\":"), DecodedBase64Module.Alphabet.DEFAULT),
      null, emptyList(), null, emptyList());
    var postFilter = PostFilterFactory.createPredicate(postModule);

    // eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9 decodes to {"alg":"HS256","typ":"JWT"} which contains both
    assertThat(postFilter.test("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9")).isFalse();

    // Test with content that has only one of the matchNot patterns
    var inputWithOnlyAlg = Base64.getEncoder().encodeToString("{\"alg\":\"HS256\"}".getBytes());
    assertThat(postFilter.test(inputWithOnlyAlg)).isFalse();

    // Test with content that has none of the matchNot patterns
    var inputWithNeither = Base64.getEncoder().encodeToString("{\"key\":\"value\"}".getBytes());
    assertThat(postFilter.test(inputWithNeither)).isTrue();
  }
}
