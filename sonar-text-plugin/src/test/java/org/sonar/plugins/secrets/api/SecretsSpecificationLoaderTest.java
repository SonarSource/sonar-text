/*
 * SonarQube Text Plugin
 * Copyright (C) 2021-2026 SonarSource Sàrl
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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.sonar.plugins.secrets.SecretsCheckList;
import org.sonar.plugins.secrets.configuration.deserialization.ReferenceTestModel;
import org.sonar.plugins.secrets.configuration.model.Rule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.plugins.secrets.SecretsSpecificationFilesDefinition.existingSecretSpecifications;
import static org.sonar.plugins.secrets.api.SecretsSpecificationLoader.DEFAULT_EXCEPTION_HANDLER;
import static org.sonar.plugins.secrets.api.SecretsSpecificationLoader.DEFAULT_SPECIFICATION_LOCATION;

@Order(1)
class SecretsSpecificationLoaderTest extends AbstractSecretsSpecificationLoaderTest {

  @Override
  public List<Class<?>> getSecretsCheckListWithSpecifications() {
    return new SecretsCheckList().checks();
  }

  @Override
  public SecretsSpecificationLoader getSecretsSpecificationLoader() {
    return new SecretsSpecificationLoader(
      DEFAULT_SPECIFICATION_LOCATION, existingSecretSpecifications(),
      exceptionCollector);
  }

  @Test
  void shouldLoadExpectedRule() {
    String specificationLocation = "secretsConfiguration/";
    Set<String> specifications = Set.of("validMinSpec.sml");
    Rule expectedRule = ReferenceTestModel.constructMinimumSpecification().getProvider().getRules().get(0);

    Rule rule = new SecretsSpecificationLoader(specificationLocation, specifications).getRulesForKey("exampleKey").get(0);

    assertThat(rule).usingRecursiveComparison().isEqualTo(expectedRule);
  }

  @Test
  void shouldLoadExpectedRuleFromMap() {
    String specificationLocation = "secretsConfiguration/";
    Set<String> specifications = Set.of("validMinSpec.sml");
    Rule expectedRule = ReferenceTestModel.constructMinimumSpecification().getProvider().getRules().get(0);

    Rule rule = new SecretsSpecificationLoader(Map.of(specificationLocation, specifications), DEFAULT_EXCEPTION_HANDLER).getRulesForKey("exampleKey").get(0);

    assertThat(rule).usingRecursiveComparison().isEqualTo(expectedRule);
  }

  @Test
  void shouldReturnNullWhenRuleIdNotPresent() {
    String specificationLocation = "secretsConfiguration/";
    Set<String> specifications = Set.of("validMinSpec.sml");

    List<Rule> rulesForKey = new SecretsSpecificationLoader(specificationLocation, specifications).getRulesForKey("notPresent");

    assertThat(rulesForKey).isEmpty();
  }

  @Test
  void duplicateKeyInRulesShouldThrowError() {
    String specificationLocation = "secretsConfiguration/";
    Set<String> specifications = Set.of("validSpecWithDuplicateRuleKeys.sml");
    List<Rule> rulesForKey = new SecretsSpecificationLoader(specificationLocation, specifications).getRulesForKey("exampleKey");

    assertThat(rulesForKey).hasSize(2);
  }

  @Test
  void shouldNotFailWhenFileNotFoundButLogShouldContainMessage() {
    String specificationLocation = "secretsConfiguration/";
    Set<String> specifications = Set.of("unknownFile.sml");

    new SecretsSpecificationLoader(specificationLocation, specifications);
    assertThat(logTester.logs()).containsExactly("DeserializationException: Could not load specification from file: unknownFile.sml");
  }

  @Test
  void shouldReturnEmptyMapWhenSpecificationsIsEmpty() {
    var specificationLoader = new SecretsSpecificationLoader("non-relevant", Collections.emptySet());

    assertThat(specificationLoader.getRulesMappedToKey()).isEqualTo(Collections.emptyMap());
  }
}
