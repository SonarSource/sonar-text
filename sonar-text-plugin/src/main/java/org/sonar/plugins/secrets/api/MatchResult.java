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

import org.sonar.plugins.secrets.api.filters.FilterOutcome;

/**
 * A match paired with the aggregate outcome from the filter pipelines that accepted it.
 *
 * @param match   the original regex match
 * @param outcome the combined filter outcome (always {@code passed}); {@code outcome.skipped()} lists any filters
 *                that were skipped, so downstream reporting can mark the finding as low-confidence.
 */
public record MatchResult(CandidateMatch match, FilterOutcome outcome) {
}
