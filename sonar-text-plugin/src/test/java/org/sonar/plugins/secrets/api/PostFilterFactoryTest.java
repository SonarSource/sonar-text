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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.sonar.plugins.secrets.configuration.deserialization.ReferenceTestModel;
import org.sonar.plugins.secrets.configuration.model.matching.filter.PostModule;

import static org.assertj.core.api.Assertions.assertThat;

class PostFilterFactoryTest {

  @Test
  void postFilterShouldReturnTrueOnHighEntropy() {
    PostModule postModule = ReferenceTestModel.constructPostModule();
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
    PostModule postModule = ReferenceTestModel.constructPostModule();
    Predicate<String> postFilter = PostFilterFactory.createPredicate(postModule);

    assertThat(postFilter.test(input)).isFalse();
  }


  @Test
  void statisticalFilterShouldReturnFalseOnLowEntropy() {
    PostModule postModule = ReferenceTestModel.constructPostModule();
    Predicate<String> predicate = PostFilterFactory.filterForStatisticalFilter(postModule.getStatisticalFilter());

    assertThat(predicate.test("rule matching pattern")).isFalse();
  }

  @Test
  void statisticalFilterShouldReturnTrueOnHighEntropy() {
    PostModule postModule = ReferenceTestModel.constructPostModule();
    Predicate<String> predicate = PostFilterFactory.filterForStatisticalFilter(postModule.getStatisticalFilter());

    assertThat(predicate.test("string with high entropy: lasdij2338f,.q29cm2acasd")).isTrue();
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
  void postFilterIsAlwaysTrueRegardlessOfInput(String input) {
    Predicate<String> postFilter = PostFilterFactory.createPredicate(null);
    assertThat(postFilter.test(input)).isTrue();
  }
}
