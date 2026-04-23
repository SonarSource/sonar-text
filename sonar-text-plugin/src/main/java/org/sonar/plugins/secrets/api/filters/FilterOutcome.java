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

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Result of evaluating a candidate against a filter pipeline.
 *
 * @param passed  whether the candidate passed the filter
 * @param skipped immutable set of skipped filters that would have rejected the candidate with the filters enabled;
 *                meaningful only when {@code passed} is {@code true}
 */
public record FilterOutcome(boolean passed, Set<SkippedFilter> skipped) {

  public static final FilterOutcome ACCEPTED = new FilterOutcome(true, Collections.emptySet());
  public static final FilterOutcome REJECTED = new FilterOutcome(false, Collections.emptySet());

  public FilterOutcome {
    skipped = skipped.isEmpty() ? Set.of() : Set.copyOf(skipped);
  }

  public static FilterOutcome passedWithSkipped(SkippedFilter skippedFilter) {
    return new FilterOutcome(true, Set.of(skippedFilter));
  }

  /**
   * Combine this outcome with another. Passed is conjunctive; skipped sets are unioned when both outcomes passed.
   */
  public FilterOutcome combine(FilterOutcome other) {
    if (!this.passed || !other.passed) {
      return REJECTED;
    }
    if (this.skipped.isEmpty()) {
      return other;
    }
    if (other.skipped.isEmpty()) {
      return this;
    }
    var union = EnumSet.noneOf(SkippedFilter.class);
    union.addAll(this.skipped);
    union.addAll(other.skipped);
    return new FilterOutcome(true, union);
  }
}
