/*
 * SonarQube Text Plugin
 * Copyright (C) 2021-2025 SonarSource Sàrl
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
package org.sonar.plugins.secrets.api;

import java.util.List;
import org.sonar.plugins.common.InputFileContext;

/**
 * A base interface for all matchers that will be used to find matches in the input file.
 */
public interface Matcher {
  /**
   * Returns a list of {@link Match matches} found in {@link InputFileContext}.
   *
   * @param fileContext the file that will be scanned.
   * @return list of matches.
   */
  List<Match> findIn(InputFileContext fileContext);
}
