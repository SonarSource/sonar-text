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

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.sonar.plugins.secrets.configuration.deserialization.ReferenceTestModel;
import org.sonar.plugins.secrets.configuration.model.matching.Matching;

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
    postModule.setPatternNot(Collections.emptyList());

    Predicate<String> postFilter = PostFilterFactory.createPredicate(postModule);

    assertThat(postFilter.test("string with low entropy")).isFalse();
  }

  @Test
  void postFilterShouldReturnTrueOnHighEntropyWhenPatternNotIsNull() {
    var postModule = ReferenceTestModel.constructPostModule();
    postModule.setPatternNot(Collections.emptyList());

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
  void statisticalFilterShouldReturnFalseOnNamedGroup() {
    var postModule = ReferenceTestModel.constructPostModule();

    Predicate<String> predicate = PostFilterFactory.filterForStatisticalFilter(postModule.getStatisticalFilter());

    assertThat(predicate.test("candidate secret")).isFalse();
  }

  @Test
  void statisticalFilterShouldReturnTrueOnNamedGroup() {
    var postModule = ReferenceTestModel.constructPostModule();
    postModule.getStatisticalFilter().setThreshold(1f);

    Predicate<String> predicate = PostFilterFactory.filterForStatisticalFilter(postModule.getStatisticalFilter());

    assertThat(predicate.test("candidate secret")).isTrue();
  }

  @Test
  void statisticalFilterShouldReturnTrueBecauseMatchingIsNull() {
    var postModule = ReferenceTestModel.constructPostModule();
    postModule.getStatisticalFilter().setThreshold(1f);

    Predicate<String> predicate = PostFilterFactory.filterForStatisticalFilter(postModule.getStatisticalFilter());

    assertThat(predicate.test("candidate secret")).isTrue();
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
    postModule.setPatternNot(Collections.emptyList());
    Predicate<String> postFilter = PostFilterFactory.createPredicate(postModule);
    assertThat(postFilter.test(input)).isTrue();
  }
}
