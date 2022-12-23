/*
 * SonarQube Text Plugin
 * Copyright (C) 2021-2022 SonarSource SA
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
package org.sonar.plugins.secrets;

import org.junit.Test;
import org.sonar.api.batch.fs.TextRange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.plugins.secrets.TestUtils.aNormalizedInputFile;

public class NormalizedInputFileTest {

  @Test
  public void should_convert_when_offset_are_identical_and_on_same_line() {
    NormalizedInputFile file = TestUtils.aNormalizedInputFile("first");

    TextRange range = file.newTextRangeFromFileOffsets(0, 1);

    assertThat(range.start().line()).isOne();
    assertThat(range.start().lineOffset()).isZero();
    assertThat(range.end().line()).isOne();
    assertThat(range.end().lineOffset()).isOne();
  }

  @Test
  public void should_convert_when_offsets_are_on_different_lines_separated_by_line_feed() {
    NormalizedInputFile file = TestUtils.aNormalizedInputFile("first\nsecond");

    TextRange range = file.newTextRangeFromFileOffsets(0, 12);

    assertThat(range.start().line()).isOne();
    assertThat(range.start().lineOffset()).isZero();
    assertThat(range.end().line()).isEqualTo(2);
    assertThat(range.end().lineOffset()).isEqualTo(6);
  }

  @Test
  public void should_convert_when_offsets_are_not_on_the_first_line() {
    NormalizedInputFile file = TestUtils.aNormalizedInputFile("first\nsecond\nthird\nfourth");

    TextRange range = file.newTextRangeFromFileOffsets(19, 25);

    assertThat(range.start().line()).isEqualTo(4);
    assertThat(range.start().lineOffset()).isZero();
    assertThat(range.end().line()).isEqualTo(4);
    assertThat(range.end().lineOffset()).isEqualTo(6);
  }
}
