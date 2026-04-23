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

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.sonar.plugins.secrets.api.filters.SkippedFilter;

import static org.assertj.core.api.Assertions.assertThat;

class SpecificationConfigurationTest {

  @Test
  void skippedFiltersDefaultsToEmpty() {
    var config = new SpecificationConfiguration(true);
    assertThat(config.skippedFilters()).isEmpty();
  }

  @Test
  void skippedFiltersCanContainEntropyFilter() {
    var config = new SpecificationConfiguration(false, Set.of(SkippedFilter.ENTROPY_FILTER), MessageFormatter.RULE_MESSAGE);
    assertThat(config.skippedFilters()).containsExactly(SkippedFilter.ENTROPY_FILTER);
  }

  @Test
  void autoTestFileDetectionEnabledConstantHasNoOverrides() {
    assertThat(SpecificationConfiguration.AUTO_TEST_FILE_DETECTION_ENABLED.skippedFilters()).isEmpty();
  }

  @Test
  void autoTestFileDetectionDisabledConstantHasNoOverrides() {
    assertThat(SpecificationConfiguration.AUTO_TEST_FILE_DETECTION_DISABLED.skippedFilters()).isEmpty();
  }
}
