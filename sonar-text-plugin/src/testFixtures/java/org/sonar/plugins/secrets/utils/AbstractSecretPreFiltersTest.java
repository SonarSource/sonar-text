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
package org.sonar.plugins.secrets.utils;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.sonar.api.internal.apachecommons.io.FileUtils;
import org.sonar.plugins.secrets.api.SecretsSpecificationLoader;
import org.sonar.plugins.secrets.configuration.model.Rule;
import org.sonar.plugins.secrets.configuration.model.matching.filter.FileFilter;
import org.sonar.plugins.secrets.configuration.model.matching.filter.PreModule;

import static org.assertj.core.api.Assertions.fail;

/**
 * Abstract test class to ensure that all secret rules have a pre.include.content filter.
 * <p>
 * This is important for performance reasons, as it allows the engine to potentially skip the file before applying the heavy regex.
 * <p>
 * The test will fail if any rule does not have a pre.include.content filter and is not in the exclusion list.
 */
public abstract class AbstractSecretPreFiltersTest {

  private final String specificationFilesLocation;
  private final Path configurationFilesPath;

  public AbstractSecretPreFiltersTest(String specificationFilesLocation) {
    this.specificationFilesLocation = specificationFilesLocation;
    this.configurationFilesPath = Path.of("src/main/resources", specificationFilesLocation);
  }

  /**
   * List of rule keys of the format "rspecKey:ruleId" that should be excluded from the test.
   */
  protected abstract Collection<String> getExcludedRuleKeys();

  @Test
  void shouldEnsureAllSecretRulesHaveContentPreFilters() {
    var secretSpecFileNames = getSecretSpecFileNames();
    var ruleKeysWithoutContentPreFilters = getRuleKeysWithoutContentPreFilters(secretSpecFileNames);
    var excludedRuleKeys = getExcludedRuleKeys();
    excludedRuleKeys.forEach(ruleKey -> {
      var ruleKeyWasInList = ruleKeysWithoutContentPreFilters.remove(ruleKey);
      if (!ruleKeyWasInList) {
        fail("Rule '%s' is in the exclusion list but does not exist or has a pre.include.content filter. " +
          "Remove it from the exclusion list in '%s'.", ruleKey, getClass().getSimpleName());
      }
    });
    ruleKeysWithoutContentPreFilters.forEach(ruleKey -> fail(
      "Rule '%s' is missing a pre.include.content filter to improve performances. " +
        "Add it or add '%s' to the exclusion list in '%s'.",
      ruleKey, ruleKey, getClass().getSimpleName()));
  }

  private Set<String> getSecretSpecFileNames() {
    var extensionsToSearchFor = new String[] {"yaml"};
    var files = FileUtils.listFiles(new File(configurationFilesPath.toUri()), extensionsToSearchFor, false);
    return files.stream().map(File::getName).collect(Collectors.toSet());
  }

  private List<String> getRuleKeysWithoutContentPreFilters(Set<String> secretSpecFileNames) {
    var specificationLoader = new SecretsSpecificationLoader(specificationFilesLocation, secretSpecFileNames);
    return specificationLoader.getRulesMappedToKey().values().stream()
      .flatMap(Collection::stream)
      .filter(this::hasNoContentPreFilter)
      .map(this::getRuleKey)
      .sorted()
      .collect(Collectors.toCollection(ArrayList::new)); // The list will get modified
  }

  private String getRuleKey(Rule rule) {
    return rule.getRspecKey() + ":" + rule.getId();
  }

  private boolean hasNoContentPreFilter(Rule rule) {
    return Optional.ofNullable(rule.getDetection().getPre())
      .map(PreModule::getInclude)
      .map(FileFilter::getContent)
      .isEmpty();
  }
}
