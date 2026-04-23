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

import java.util.List;
import org.sonar.plugins.common.InputFileContext;
import org.sonar.plugins.secrets.api.filters.FilterOutcome;

/**
 * A base interface for all matchers that run a complete matching pipeline against an input file.
 * Implementations return only matches that survived every pre- and post-filter stage, each paired
 * with the {@link FilterOutcome} describing how they got there.
 */
public interface Matcher {
  /**
   * Runs the full matching pipeline against {@link InputFileContext} and returns the accepted matches.
   *
   * @param fileContext the file that will be scanned.
   * @return list of matches that passed the pipeline, each carrying its filter outcome.
   */
  List<MatchResult> findIn(InputFileContext fileContext);
}
