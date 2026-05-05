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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FilterOutcomeTest {

  @Test
  void combineReturnsRejectedWhenThisIsRejected() {
    var other = FilterOutcome.passedWithSkipped(SkippedFilter.ENTROPY_FILTER);
    assertThat(FilterOutcome.REJECTED.combine(other)).isEqualTo(FilterOutcome.REJECTED);
  }

  @Test
  void combineReturnsRejectedWhenOtherIsRejected() {
    var passed = FilterOutcome.passedWithSkipped(SkippedFilter.ENTROPY_FILTER);
    assertThat(passed.combine(FilterOutcome.REJECTED)).isEqualTo(FilterOutcome.REJECTED);
  }

  @Test
  void combineReturnsOtherWhenThisHasNoSkippedFilters() {
    var other = FilterOutcome.passedWithSkipped(SkippedFilter.ENTROPY_FILTER);
    assertThat(FilterOutcome.ACCEPTED.combine(other)).isSameAs(other);
  }

  @Test
  void combineReturnsThisWhenOtherHasNoSkippedFilters() {
    var current = FilterOutcome.passedWithSkipped(SkippedFilter.TEST_FILES_FILTER);
    assertThat(current.combine(FilterOutcome.ACCEPTED)).isSameAs(current);
  }

  @Test
  void combineUnionsSkippedFiltersWhenBothPassedWithSkipped() {
    var entropy = FilterOutcome.passedWithSkipped(SkippedFilter.ENTROPY_FILTER);
    var testFiles = FilterOutcome.passedWithSkipped(SkippedFilter.TEST_FILES_FILTER);

    var combined = entropy.combine(testFiles);

    assertThat(combined.passed()).isTrue();
    assertThat(combined.skipped()).containsExactlyInAnyOrder(SkippedFilter.ENTROPY_FILTER, SkippedFilter.TEST_FILES_FILTER);
  }

  @Test
  void combineWithSameSkippedFilterDoesNotDuplicate() {
    var entropy = FilterOutcome.passedWithSkipped(SkippedFilter.ENTROPY_FILTER);

    var combined = entropy.combine(entropy);

    assertThat(combined.passed()).isTrue();
    assertThat(combined.skipped()).containsExactly(SkippedFilter.ENTROPY_FILTER);
  }

  @Test
  void combineWithBothAcceptedReturnsAccepted() {
    assertThat(FilterOutcome.ACCEPTED.combine(FilterOutcome.ACCEPTED)).isEqualTo(FilterOutcome.ACCEPTED);
  }
}
