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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonar.plugins.secrets.SecretsCheckList;
import org.sonar.plugins.secrets.configuration.deserialization.ReferenceTestModel;
import org.sonar.plugins.secrets.configuration.model.Rule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.plugins.secrets.SecretsSpecificationFilesDefinition.existingSecretSpecifications;
import static org.sonar.plugins.secrets.api.SecretsSpecificationLoader.DEFAULT_EXCEPTION_HANDLER;
import static org.sonar.plugins.secrets.api.SecretsSpecificationLoader.DEFAULT_SPECIFICATION_LOCATION;

@Order(1)
class SecretsSpecificationLoaderTest {

  @RegisterExtension
  LogTesterJUnit5 logTester = new LogTesterJUnit5();

  @Test
  void shouldLoadRulesWithoutErrors() {
    var errors = new HashSet<Throwable>();
    var specificationLoader = new SecretsSpecificationLoader(
      DEFAULT_SPECIFICATION_LOCATION, existingSecretSpecifications(),
      (e, specificationFileName) -> errors.add(e));
    Map<String, List<Rule>> rulesMappedToKey = specificationLoader.getRulesMappedToKey();

    assertThat(errors)
      .as("No errors are expected during loading of specifications")
      .isEmpty();

    var numberOfSpecificationBasedChecks = new SecretsCheckList().checks().stream()
      .filter(SpecificationBasedCheck.class::isAssignableFrom)
      .count();
    assertThat(rulesMappedToKey.values()).hasSize((int) numberOfSpecificationBasedChecks);
    assertThat(logTester.getLogs()).isEmpty();
  }

  @Test
  void shouldLoadExpectedRule() {
    String specificationLocation = "secretsConfiguration/";
    Set<String> specifications = Set.of("validMinSpec.yaml");
    Rule expectedRule = ReferenceTestModel.constructMinimumSpecification().getProvider().getRules().get(0);

    Rule rule = new SecretsSpecificationLoader(specificationLocation, specifications).getRulesForKey("exampleKey").get(0);

    assertThat(rule).usingRecursiveComparison().isEqualTo(expectedRule);
  }

  @Test
  void shouldLoadExpectedRuleFromMap() {
    String specificationLocation = "secretsConfiguration/";
    Set<String> specifications = Set.of("validMinSpec.yaml");
    Rule expectedRule = ReferenceTestModel.constructMinimumSpecification().getProvider().getRules().get(0);

    Rule rule = new SecretsSpecificationLoader(Map.of(specificationLocation, specifications), DEFAULT_EXCEPTION_HANDLER).getRulesForKey("exampleKey").get(0);

    assertThat(rule).usingRecursiveComparison().isEqualTo(expectedRule);
  }

  @Test
  void shouldReturnNullWhenRuleIdNotPresent() {
    String specificationLocation = "secretsConfiguration/";
    Set<String> specifications = Set.of("validMinSpec.yaml");

    List<Rule> rulesForKey = new SecretsSpecificationLoader(specificationLocation, specifications).getRulesForKey("notPresent");

    assertThat(rulesForKey).isEmpty();
  }

  @Test
  void duplicateKeyInRulesShouldThrowError() {
    String specificationLocation = "secretsConfiguration/";
    Set<String> specifications = Set.of("validSpecWithDuplicateRuleKeys.yaml");
    List<Rule> rulesForKey = new SecretsSpecificationLoader(specificationLocation, specifications).getRulesForKey("exampleKey");

    assertThat(rulesForKey).hasSize(2);
  }

  @Test
  void shouldNotFailWhenFileNotFoundButLogShouldContainMessage() {
    String specificationLocation = "secretsConfiguration/";
    Set<String> specifications = Set.of("unknownFile.yaml");

    new SecretsSpecificationLoader(specificationLocation, specifications);
    assertThat(logTester.logs()).containsExactly("DeserializationException: Could not load specification from file: unknownFile.yaml");
  }

  @Test
  void shouldReturnEmptyMapWhenSpecificationsIsEmpty() {
    var specificationLoader = new SecretsSpecificationLoader("non-relevant", Collections.emptySet());

    assertThat(specificationLoader.getRulesMappedToKey()).isEqualTo(Collections.emptyMap());
  }
}
