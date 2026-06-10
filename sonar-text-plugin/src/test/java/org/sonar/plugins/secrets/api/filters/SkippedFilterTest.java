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
package org.sonar.plugins.secrets.api.filters;

import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SkippedFilterTest {

  @Test
  void shouldReturnMessageUnchangedWhenNoFiltersSkipped() {
    assertThat(SkippedFilter.appendLowConfidenceSuffix("base message", Set.of()))
      .isEqualTo("base message");
  }

  @Test
  void shouldAppendSingleFilterNameWhenOneFilterSkipped() {
    assertThat(SkippedFilter.appendLowConfidenceSuffix("base message", Set.of(SkippedFilter.TEST_FILES_FILTER)))
      .isEqualTo("base message (low-confidence match, disabled filters: automatic test file detection)");
  }

  @Test
  void shouldGroupMultipleFilterNamesUnderSingleLowConfidencePrefix() {
    var skipped = EnumSet.of(SkippedFilter.ENTROPY_FILTER, SkippedFilter.TEST_FILES_FILTER);
    var amended = SkippedFilter.appendLowConfidenceSuffix("base message", skipped);
    assertThat(amended)
      .isEqualTo("base message (low-confidence match, disabled filters: entropy, automatic test file detection)");
    assertThat(amended.split("low-confidence match")).hasSize(2);
  }

  @Test
  void shouldAppendKnownFakeSecretFilterName() {
    assertThat(SkippedFilter.appendLowConfidenceSuffix("base message", Set.of(SkippedFilter.KNOWN_FAKE_SECRET_FILTER)))
      .isEqualTo("base message (low-confidence match, disabled filters: known fake secrets)");
  }

  @Test
  void shouldGroupAllThreeFilterNamesUnderSingleLowConfidencePrefix() {
    var skipped = EnumSet.of(SkippedFilter.ENTROPY_FILTER, SkippedFilter.KNOWN_FAKE_SECRET_FILTER, SkippedFilter.TEST_FILES_FILTER);
    var amended = SkippedFilter.appendLowConfidenceSuffix("base message", skipped);
    assertThat(amended)
      .isEqualTo("base message (low-confidence match, disabled filters: entropy, known fake secrets, automatic test file detection)");
  }
}
