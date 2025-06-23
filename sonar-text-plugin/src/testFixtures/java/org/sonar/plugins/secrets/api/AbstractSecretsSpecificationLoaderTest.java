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

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonar.plugins.secrets.configuration.model.Rule;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class AbstractSecretsSpecificationLoaderTest {

  @RegisterExtension
  LogTesterJUnit5 logTester = new LogTesterJUnit5();

  public final Set<Throwable> collectedErrors = new HashSet<>();
  public final SecretsSpecificationLoader.ExceptionHandler exceptionCollector = (e, specificationFileName) -> collectedErrors.add(e);

  public abstract List<Class<?>> getSecretsCheckListWithSpecifications();

  public abstract SecretsSpecificationLoader getSecretsSpecificationLoader();

  @Test
  void shouldLoadRulesWithoutErrors() {
    var specificationLoader = getSecretsSpecificationLoader();
    Map<String, List<Rule>> rulesMappedToKey = specificationLoader.getRulesMappedToKey();

    assertThat(collectedErrors)
      .as("No errors are expected during loading of specifications")
      .isEmpty();

    var numberOfSpecificationBasedChecks = getSecretsCheckListWithSpecifications().stream()
      .filter(SpecificationBasedCheck.class::isAssignableFrom)
      .count();
    assertThat(rulesMappedToKey.values()).hasSize((int) numberOfSpecificationBasedChecks);
    assertThat(logTester.getLogs()).isEmpty();
  }
}
