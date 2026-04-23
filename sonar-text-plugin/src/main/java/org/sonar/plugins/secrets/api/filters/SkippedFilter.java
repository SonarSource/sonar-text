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

/**
 * Catalog of filters a caller may request to skip. When a filter is skipped, a candidate that would have been
 * rejected by it is instead passed through and the resulting finding is tagged with this value so downstream
 * reporting can mark it as low-confidence.
 *
 * <p>Adding a new skippable filter is a single-line change here plus wiring in the handler (post-filter) or factory
 * (pre-filter) that owns the affected filter.
 */
public enum SkippedFilter {
  ENTROPY_FILTER("low-confidence match, entropy filter is disabled");

  private final String lowConfidenceLabel;

  SkippedFilter(String lowConfidenceLabel) {
    this.lowConfidenceLabel = lowConfidenceLabel;
  }

  public String lowConfidenceLabel() {
    return lowConfidenceLabel;
  }
}
