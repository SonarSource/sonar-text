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
package org.sonar.plugins.secrets.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.sonar.plugins.secrets.api.SecretsSpecificationLoader;
import org.sonar.plugins.secrets.configuration.model.Rule;
import org.sonar.plugins.secrets.configuration.model.matching.AuxiliaryPattern;
import org.sonar.plugins.secrets.configuration.model.matching.BooleanCombination;
import org.sonar.plugins.secrets.configuration.model.matching.Match;

import static org.assertj.core.api.Assertions.fail;

/**
 * Abstract test class to ensure that no secret rules use maxCharDistance: 0 in auxiliary patterns.
 * <p>
 * Using maxCharDistance: 0 is an anti-pattern that significantly degrades performance.
 * Instead, patterns with maxCharDistance: 0 should be merged directly into the main pattern.
 */
public abstract class AbstractSecretMaxCharDistanceTest {

  private final String specificationFilesLocation;

  public AbstractSecretMaxCharDistanceTest(String specificationFilesLocation) {
    this.specificationFilesLocation = specificationFilesLocation;
  }

  /**
   * Set of specification file names (e.g., "aws.yaml", "github.yaml") to test.
   */
  protected abstract Set<String> getSpecificationFiles();

  /**
   * List of rule keys of the format "rspecKey:ruleId" that should be excluded from the test.
   * These will be allowed to have maxCharDistance: 0 temporarily during migration.
   */
  protected abstract Collection<String> getExcludedRuleKeys();

  @Test
  void shouldNotUseMaxCharDistanceZero() {
    var secretSpecFileNames = getSpecificationFiles();
    var specificationLoader = new SecretsSpecificationLoader(specificationFilesLocation, secretSpecFileNames);

    var rulesWithMaxCharDistanceZero = specificationLoader.getRulesMappedToKey().values().stream()
      .flatMap(Collection::stream)
      .filter(this::hasMaxCharDistanceZero)
      .map(this::getRuleKey)
      .sorted()
      .collect(Collectors.toCollection(ArrayList::new));

    var excludedRuleKeys = getExcludedRuleKeys();
    excludedRuleKeys.forEach(ruleKey -> {
      var ruleKeyWasInList = rulesWithMaxCharDistanceZero.remove(ruleKey);
      if (!ruleKeyWasInList) {
        fail("Rule '%s' is in the exclusion list but does not use maxCharDistance: 0. " +
          "Remove it from the exclusion list in '%s'.", ruleKey, getClass().getSimpleName());
      }
    });

    if (!rulesWithMaxCharDistanceZero.isEmpty()) {
      var message = "The following rules use maxCharDistance: 0 which is an anti-pattern:\n" +
        String.join("\n", rulesWithMaxCharDistanceZero) +
        "\n\nMerge the auxiliary pattern directly into the main pattern instead.\n" +
        "If it is not possible, add the rules to the exclusion list in " + this.getClass().getCanonicalName() + ".";
      fail(message);
    }
  }

  private String getRuleKey(Rule rule) {
    return rule.getRspecKey() + ":" + rule.getId();
  }

  private boolean hasMaxCharDistanceZero(Rule rule) {
    var matching = rule.getDetection().getMatching();
    if (matching == null) {
      return false;
    }

    var context = matching.getContext();
    if (context == null) {
      return false;
    }

    return hasMaxCharDistanceZeroInMatch(context);
  }

  private boolean hasMaxCharDistanceZeroInMatch(Match match) {
    if (match instanceof AuxiliaryPattern auxiliaryPattern) {
      var maxCharDistance = auxiliaryPattern.getMaxCharacterDistance();
      return maxCharDistance != null && maxCharDistance == 0;
    } else if (match instanceof BooleanCombination booleanCombination) {
      return booleanCombination.getMatches().stream()
        .anyMatch(this::hasMaxCharDistanceZeroInMatch);
    }
    return false;
  }
}
