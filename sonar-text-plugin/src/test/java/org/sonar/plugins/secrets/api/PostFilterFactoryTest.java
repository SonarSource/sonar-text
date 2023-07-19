/*
 * SonarQube Text Plugin
 * Copyright (C) 2021-2023 SonarSource SA
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

import java.util.function.Predicate;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.sonar.plugins.secrets.configuration.deserialization.ReferenceTestModel;
import org.sonar.plugins.secrets.configuration.model.matching.Matching;
import org.sonar.plugins.secrets.configuration.model.matching.filter.PostModule;

import static org.assertj.core.api.Assertions.assertThat;

class PostFilterFactoryTest {

  private static Matching matching;

  @BeforeAll
  public static void initialize() {
    matching = new Matching();
    matching.setPattern("\\b(?<groupName>candidate) secret\\b");
  }

  @Test
  void postFilterShouldReturnTrueOnHighEntropy() {
    PostModule postModule = ReferenceTestModel.constructPostModule();
    postModule.getStatisticalFilter().setInputString(null);

    Predicate<String> postFilter = PostFilterFactory.createPredicate(postModule, matching);

    assertThat(postFilter.test("candidate secret with high entropy: lasdij2338f,.q29cm2acasd")).isTrue();
  }

  @ParameterizedTest
  @ValueSource(strings = {
    "candidate secret with low entropy",
    "candidate secret with low entropy and patternNot:EXAMPLEKEY",
    "candidate secret with high entropy: lasdij2338f,.q29cm2acasd has patternNot:EXAMPLEKEY"
  })
  void postFilterShouldReturnFalse(String input) {
    PostModule postModule = ReferenceTestModel.constructPostModule();
    postModule.getStatisticalFilter().setInputString(null);

    Predicate<String> postFilter = PostFilterFactory.createPredicate(postModule, matching);

    assertThat(postFilter.test(input)).isFalse();
  }

  @Test
  void postFilterShouldReturnFalseOnLowEntropyWhenPatternNotIsNull() {
    PostModule postModule = ReferenceTestModel.constructPostModule();
    postModule.getStatisticalFilter().setInputString(null);
    postModule.setPatternNot(null);

    Predicate<String> postFilter = PostFilterFactory.createPredicate(postModule, matching);

    assertThat(postFilter.test("string with low entropy")).isFalse();
  }

  @Test
  void postFilterShouldReturnTrueOnHighEntropyWhenPatternNotIsNull() {
    PostModule postModule = ReferenceTestModel.constructPostModule();
    postModule.getStatisticalFilter().setInputString(null);
    postModule.setPatternNot(null);

    Predicate<String> postFilter = PostFilterFactory.createPredicate(postModule, matching);

    assertThat(postFilter.test("rule matching EXAMPLEKEY pattern with high entropy: lasdij2338f,.q29cm2acasd")).isTrue();
  }

  @Test
  void postFilterShouldReturnFalseOnLowEntropyWhenStatisticalFilterIsNull() {
    PostModule postModule = ReferenceTestModel.constructPostModule();
    postModule.getStatisticalFilter().setInputString(null);
    postModule.setStatisticalFilter(null);

    Predicate<String> postFilter = PostFilterFactory.createPredicate(postModule, matching);

    assertThat(postFilter.test("rule matching EXAMPLEKEY pattern")).isFalse();
  }

  @Test
  void postFilterShouldReturnTrueOnHighEntropyWhenStatisticalFilterIsNull() {
    PostModule postModule = ReferenceTestModel.constructPostModule();
    postModule.getStatisticalFilter().setInputString(null);
    postModule.setStatisticalFilter(null);

    Predicate<String> postFilter = PostFilterFactory.createPredicate(postModule, matching);

    assertThat(postFilter.test("string with high entropy: lasdij2338f,.q29cm2acasd")).isTrue();
  }

  @Test
  void statisticalFilterShouldReturnFalseOnLowEntropy() {
    PostModule postModule = ReferenceTestModel.constructPostModule();
    postModule.getStatisticalFilter().setInputString(null);

    Predicate<String> predicate = PostFilterFactory.filterForStatisticalFilter(postModule.getStatisticalFilter(), matching);

    assertThat(predicate.test("rule matching pattern")).isFalse();
  }

  @Test
  void statisticalFilterShouldReturnTrueOnHighEntropy() {
    PostModule postModule = ReferenceTestModel.constructPostModule();
    postModule.getStatisticalFilter().setInputString(null);

    Predicate<String> predicate = PostFilterFactory.filterForStatisticalFilter(postModule.getStatisticalFilter(), matching);

    assertThat(predicate.test("string with high entropy: lasdij2338f,.q29cm2acasd")).isTrue();
  }

  @Test
  void statisticalFilterShouldReturnTrueOnNamedGroup() {
    PostModule postModule = ReferenceTestModel.constructPostModule();

    Predicate<String> predicate = PostFilterFactory.filterForStatisticalFilter(postModule.getStatisticalFilter(), matching);

    assertThat(predicate.test("candidate secret")).isFalse();
  }

  @Test
  void statisticalFilterShouldReturnFalseOnNamedGroup() {
    PostModule postModule = ReferenceTestModel.constructPostModule();
    postModule.getStatisticalFilter().setThreshold(1f);

    Predicate<String> predicate = PostFilterFactory.filterForStatisticalFilter(postModule.getStatisticalFilter(), matching);

    assertThat(predicate.test("candidate secret")).isTrue();
  }


  @Test
  void patternNotFilterShouldReturnTrue() {
    Predicate<String> predicate = PostFilterFactory.filterForPatternNot("patternNot");

    assertThat(predicate.test("candidate secret")).isTrue();
  }

  @Test
  void patternNotFilterShouldReturnFalse() {
    Predicate<String> predicate = PostFilterFactory.filterForPatternNot("patternNot");

    assertThat(predicate.test("candidate secret with patternNot")).isFalse();
  }

  @ParameterizedTest
  @ValueSource(strings = {
    "candidate secret with low entropy",
    "candidate secret with low entropy and patternNot:EXAMPLEKEY",
    "candidate secret with high entropy: lasdij2338f,.q29cm2acasd has patternNot:EXAMPLEKEY",
    "candidate secret with high entropy: lasdij2338f,.q29cm2acasd"
  })
  void postFilterIsAlwaysTrueRegardlessOfInputWhenInputIsNull(String input) {
    Predicate<String> postFilter = PostFilterFactory.createPredicate(null, matching);
    assertThat(postFilter.test(input)).isTrue();
  }

  @ParameterizedTest
  @ValueSource(strings = {
    "candidate secret with low entropy",
    "candidate secret with low entropy and patternNot:EXAMPLEKEY",
    "candidate secret with high entropy: lasdij2338f,.q29cm2acasd has patternNot:EXAMPLEKEY",
    "candidate secret with high entropy: lasdij2338f,.q29cm2acasd"
  })
  void postFilterIsAlwaysTrueRegardlessOfInputWhenStatFilterAndPatternNotIsNull(String input) {
    PostModule postModule = ReferenceTestModel.constructPostModule();
    postModule.setStatisticalFilter(null);
    postModule.setPatternNot(null);
    Predicate<String> postFilter = PostFilterFactory.createPredicate(postModule, matching);
    assertThat(postFilter.test(input)).isTrue();
  }

  @MethodSource
  @ParameterizedTest(name = "{0}")
  void testEntropyInputCalculation(String testName, String statisticalFilterInputString, String candidateSecret,
    String expectedEntropyInput) {
    String calculatedEntropyInput = PostFilterFactory.calculateEntropyInputBasedOnNamedGroup(statisticalFilterInputString,
      candidateSecret, matching);

    assertThat(calculatedEntropyInput).isEqualTo(expectedEntropyInput);
  }

  private static Stream<Arguments> testEntropyInputCalculation() {
    return Stream.of(
      Arguments.of("should return captured group as groupName exists in pattern", "groupName", "candidate secret", "candidate"),
      Arguments.of("should fallback to candidate secret as groupName doesn't exist", "notExistingGroupName", "candidate secret",
        "candidate secret"),

      // this should not happen in real detection cases
      Arguments.of("candidate secret couldn't be found with pattern", "groupName", "something else", "something else")
    );
  }

}
