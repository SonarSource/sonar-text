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
package org.sonar.plugins.secrets.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.sonar.plugins.secrets.api.SecretsSpecificationLoader;
import org.sonar.plugins.secrets.configuration.model.Rule;
import org.sonar.plugins.secrets.configuration.model.matching.filter.PreModule;

import static org.assertj.core.api.Assertions.fail;

/**
 * Abstract test class to ensure that all secret rules have a pre.include filter.
 * <p>
 * This is important for performance reasons, as it allows the engine to potentially skip the file before applying the heavy regex.
 * <p>
 * The test will fail if any rule does not have a pre.include filter and is not in the exclusion list.
 */
public abstract class AbstractSecretPreFiltersTest {

  private final String specificationFilesLocation;

  public AbstractSecretPreFiltersTest(String specificationFilesLocation) {
    this.specificationFilesLocation = specificationFilesLocation;
  }

  /**
   * List of rule keys of the format "rspecKey:ruleId" that should be excluded from the test.
   */
  protected abstract Collection<String> getExcludedRuleKeys();

  /**
   * Set of specification file names (e.g., "aws.sml", "github.sml") to test.
   */
  protected abstract Set<String> getSpecificationFiles();

  @Test
  void shouldEnsureAllSecretRulesIncludePreFilters() {
    var secretSpecFileNames = getSpecificationFiles();
    var ruleKeysWithoutIncludePreFilters = getRuleKeysWithoutIncludePreFilters(secretSpecFileNames);
    var excludedRuleKeys = getExcludedRuleKeys();
    excludedRuleKeys.forEach(ruleKey -> {
      var ruleKeyWasInList = ruleKeysWithoutIncludePreFilters.remove(ruleKey);
      if (!ruleKeyWasInList) {
        fail("Rule '%s' is in the exclusion list but does not exist or has a pre.include filter. " +
          "Remove it from the exclusion list in '%s'.", ruleKey, getClass().getSimpleName());
      }
    });
    ruleKeysWithoutIncludePreFilters.forEach(ruleKey -> fail(
      "Rule '%s' is missing a pre.include filter to improve performances. " +
        "Add it or add '%s' to the exclusion list in '%s'.",
      ruleKey, ruleKey, getClass().getSimpleName()));
  }

  private List<String> getRuleKeysWithoutIncludePreFilters(Set<String> secretSpecFileNames) {
    var specificationLoader = new SecretsSpecificationLoader(specificationFilesLocation, secretSpecFileNames);
    return specificationLoader.getRulesMappedToKey().values().stream()
      .flatMap(Collection::stream)
      .filter(this::hasNoIncludePreFilter)
      .map(this::getRuleKey)
      .sorted()
      .collect(Collectors.toCollection(ArrayList::new)); // The list will get modified
  }

  private String getRuleKey(Rule rule) {
    return rule.getRspecKey() + ":" + rule.getId();
  }

  private boolean hasNoIncludePreFilter(Rule rule) {
    return Optional.ofNullable(rule.getDetection().getPre())
      .map(PreModule::getInclude)
      .isEmpty();
  }
}
