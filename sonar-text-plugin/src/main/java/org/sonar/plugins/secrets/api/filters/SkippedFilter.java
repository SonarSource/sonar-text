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

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Catalog of filters a caller may request to skip. When a filter is skipped, a candidate that would have been
 * rejected by it is instead passed through and the resulting finding is tagged with this value so downstream
 * reporting can mark it as low-confidence.
 *
 * <p>Adding a new skippable filter is a single-line change here plus wiring in the handler (post-filter) or factory
 * (pre-filter) that owns the affected filter.
 *
 * <p>Each value carries a short filter name (e.g. {@code "entropy"}). Issue messages compose these names into a
 * single {@code "low-confidence match, disabled filters: <name1>, <name2>"} suffix, so multiple skipped filters do
 * not duplicate the prefix.
 */
public enum SkippedFilter {
  ENTROPY_FILTER("entropy"),
  KNOWN_FAKE_SECRET_FILTER("known fake secrets"),
  TEST_FILES_FILTER("automatic test file detection");

  public static final String LOW_CONFIDENCE_PREFIX = "low-confidence match";

  private final String filterName;

  SkippedFilter(String filterName) {
    this.filterName = filterName;
  }

  public String filterName() {
    return filterName;
  }

  /**
   * Append a low-confidence suffix to {@code message}, listing the names of the {@code skipped} filters. Returns
   * {@code message} unchanged when {@code skipped} is empty.
   *
   * <p>Format: {@code "<message> (low-confidence match, disabled filters: <name1>, <name2>)"}. Names are emitted
   * in enum declaration order for stable output regardless of the iteration order of the input set.
   *
   * @param message the original issue message
   * @param skipped filters that were skipped during evaluation
   * @return the message, optionally amended with a low-confidence suffix
   */
  public static String appendLowConfidenceSuffix(String message, Set<SkippedFilter> skipped) {
    if (skipped.isEmpty()) {
      return message;
    }
    var names = skipped.stream()
      .sorted()
      .map(SkippedFilter::filterName)
      .collect(Collectors.joining(", "));
    return message + " (" + LOW_CONFIDENCE_PREFIX + ", disabled filters: " + names + ")";
  }
}
