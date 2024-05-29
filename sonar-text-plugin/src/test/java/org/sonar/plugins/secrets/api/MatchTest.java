/*
 * SonarQube Text Plugin
 * Copyright (C) 2021-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
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

    assertThat(match.getText()).isEqualTo(text);
    assertThat(match.getFileStartOffset()).isEqualTo(fileStartOffset);
    assertThat(match.getFileEndOffset()).isEqualTo(fileEndOffset);
    assertThat(match).hasToString("Match{text='text', fileStartOffset=10, fileEndOffset=20}");
  }
}
