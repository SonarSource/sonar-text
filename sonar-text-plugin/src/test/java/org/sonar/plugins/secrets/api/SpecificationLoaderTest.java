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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonar.plugins.secrets.SecretsRulesDefinition;
import org.sonar.plugins.secrets.configuration.deserialization.ReferenceTestModel;
import org.sonar.plugins.secrets.configuration.model.Rule;

import static org.assertj.core.api.Assertions.assertThat;

class SpecificationLoaderTest {

  @RegisterExtension
  LogTesterJUnit5 logTester = new LogTesterJUnit5();

  @Test
  void shouldLoadRulesWithoutErrors() {
    SpecificationLoader specificationLoader = new SpecificationLoader();
    Map<String, List<Rule>> rulesMappedToKey = specificationLoader.getRulesMappedToKey();

    assertThat(rulesMappedToKey.values()).hasSize(SecretsRulesDefinition.checks().size());
    assertThat(logTester.getLogs()).isEmpty();
  }

  @Test
  void shouldLoadExpectedRule() {
    String specificationLocation = "secretsConfiguration/";
    Set<String> specifications = Set.of("validMinSpec.yaml");
    Rule expectedRule = ReferenceTestModel.constructMinimumSpecification().getProvider().getRules().get(0);

    Rule rule = new SpecificationLoader(specificationLocation, specifications).getRulesForKey("exampleKey").get(0);

    assertThat(rule).usingRecursiveComparison().isEqualTo(expectedRule);
  }

  @Test
  void shouldReturnNullWhenRuleIdNotPresent() {
    String specificationLocation = "secretsConfiguration/";
    Set<String> specifications = Set.of("validMinSpec.yaml");

    List<Rule> rulesForKey = new SpecificationLoader(specificationLocation, specifications).getRulesForKey("notPresent");

    assertThat(rulesForKey).isEmpty();
  }

  @Test
  void duplicateKeyInRulesShouldThrowError() {
    String specificationLocation = "secretsConfiguration/";
    Set<String> specifications = Set.of("validSpecWithDuplicateRuleKeys.yaml");
    List<Rule> rulesForKey = new SpecificationLoader(specificationLocation, specifications).getRulesForKey("exampleKey");

    assertThat(rulesForKey).hasSize(2);
  }

  @Test
  void shouldNotFailWhenFileNotFoundButLogShouldContainMessage() {
    String specificationLocation = "secretsConfiguration/";
    Set<String> specifications = Set.of("unknownFile.yaml");

    new SpecificationLoader(specificationLocation, specifications);
    assertThat(logTester.logs()).containsExactly("DeserializationException: Could not load specification from file: unknownFile.yaml");
  }
}
