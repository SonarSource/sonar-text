/*
 * SonarQube Text Plugin
 * Copyright (C) 2021-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
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

import java.util.Map;

/**
 * Result of applying pattern matcher to a text.
 *
 * @param text            matched text
 * @param fileStartOffset inclusive start offset
 * @param fileEndOffset   exclusive end offset
 * @param groups          map containing matches per named group
 */
public record Match(String text, int fileStartOffset, int fileEndOffset, Map<String, Match> groups) {

  @Override
  public String toString() {
    return "Match{" +
      "text='" + text + '\'' +
      ", fileStartOffset=" + fileStartOffset +
      ", fileEndOffset=" + fileEndOffset +
      '}';
  }
}
