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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MatchTest {

  @Test
  void constructorShouldFunctionAsExpected() {
    String text = "text";
    int fileStartOffset = 10;
    int fileEndOffset = 20;
    Match match = new Match(text, fileStartOffset, fileEndOffset);

    assertThat(match.text()).isEqualTo(text);
    assertThat(match.fileStartOffset()).isEqualTo(fileStartOffset);
    assertThat(match.fileEndOffset()).isEqualTo(fileEndOffset);
    assertThat(match).hasToString("Match{text='text', fileStartOffset=10, fileEndOffset=20}");
  }
}
