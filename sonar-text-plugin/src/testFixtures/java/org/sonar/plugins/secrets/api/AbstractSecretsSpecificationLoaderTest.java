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
package org.sonar.plugins.secrets.api;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonar.plugins.secrets.configuration.model.Rule;
import org.sonar.plugins.secrets.configuration.model.Selectivity;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class AbstractSecretsSpecificationLoaderTest {

  @RegisterExtension
  LogTesterJUnit5 logTester = new LogTesterJUnit5();

  public final Set<Throwable> collectedErrors = new HashSet<>();
  public final SecretsSpecificationLoader.ExceptionHandler exceptionCollector = (e, specificationFileName) -> collectedErrors.add(e);

  public abstract List<Class<?>> getSecretsCheckListWithSpecifications();

  public abstract SecretsSpecificationLoader getSecretsSpecificationLoader();

  @Test
  void shouldIncludeEveryAnalyzerGenericRuleInGenericSecretRuleCoverage() {
    var specificationLoader = getSecretsSpecificationLoader();
    assertThat(collectedErrors)
      .as("All specifications must load before checking analyzer-generic rule coverage")
      .isEmpty();

    var analyzerGenericRuleKeys = specificationLoader.getRulesMappedToKey().values().stream()
      .flatMap(List::stream)
      .filter(rule -> rule.getSelectivity() == Selectivity.ANALYZER_GENERIC)
      .map(Rule::getRspecKey)
      .distinct()
      .toList();

    assertThat(analyzerGenericRuleKeys)
      .as("Every analyzerGeneric rule must be included in GenericSecretRuleCoverage.ANALYZER_GENERIC_RULE_KEYS")
      .isSubsetOf(GenericSecretRuleCoverage.ANALYZER_GENERIC_RULE_KEYS);
  }

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
