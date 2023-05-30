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

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.utils.log.LogTesterJUnit5;
import org.sonar.plugins.secrets.configuration.deserialization.ReferenceTestModel;
import org.sonar.plugins.secrets.configuration.model.Rule;
import org.sonar.plugins.secrets.configuration.validation.SchemaValidationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

class SpecificationLoaderTest {

  @RegisterExtension
  LogTesterJUnit5 logTester = new LogTesterJUnit5();

  @Test
  void initializingDefaultLoaderShouldNotThrowAnException() {
    assertThatNoException().isThrownBy(() -> new SpecificationLoader().getRuleForKey("exampleKey"));
  }

  @Test
  void shouldLoadExpectedRule() {
    String specificationLocation = "secretsConfiguration/";
    Set<String> specifications = Set.of("validMinSpec.yaml");
    Rule expectedRule = ReferenceTestModel.constructMinimumSpecification().getProvider().getRules().get(0);

    Rule rule = new SpecificationLoader(specificationLocation, specifications).getRuleForKey("exampleKey");

    assertThat(rule).usingRecursiveComparison().isEqualTo(expectedRule);
  }

  @Test
  void shouldReturnNullWhenRuleIdNotPresent() {
    String specificationLocation = "secretsConfiguration/";
    Set<String> specifications = Set.of("validMinSpec.yaml");

    Rule rule = new SpecificationLoader(specificationLocation, specifications).getRuleForKey("notPresent");

    assertThat(rule).isNull();
  }

  @Test
  void duplicateKeyInRulesShouldThrowError() {
    String specificationLocation = "secretsConfiguration/";
    Set<String> specifications = Set.of("invalidSpecWithDuplicateRuleKey.yaml");

    assertThatExceptionOfType(SchemaValidationException.class)
      .isThrownBy(() -> new SpecificationLoader(specificationLocation, specifications))
      .withMessage("RuleKey exampleKey was used multiple times, when it should be unique across all specification files.");
  }

  @Test
  void shouldNotFailWhenFileNotFoundButLogShouldContainMessage() {
    String specificationLocation = "secretsConfiguration/";
    Set<String> specifications = Set.of("unknownFile.yaml");

    new SpecificationLoader(specificationLocation, specifications);
    assertThat(logTester.logs()).containsExactly("Could not load specification from file: unknownFile.yaml");
  }
}
