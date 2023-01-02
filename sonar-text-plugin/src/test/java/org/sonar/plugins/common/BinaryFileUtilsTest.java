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
package org.sonar.plugins.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.plugins.common.BinaryFileUtils.hasControlCharacters;

class BinaryFileUtilsTest {

  @Test
  void has_control_characters() {
    assertThat(hasControlCharacters("")).isFalse();
    assertThat(hasControlCharacters("abc\tdef\r\n")).isFalse();
    assertThat(hasControlCharacters("abc\tdef\r\n\u00FF")).isFalse();
    assertThat(hasControlCharacters("\0abc")).isTrue();
    assertThat(hasControlCharacters("abc\u0007def")).isTrue();
    assertThat(hasControlCharacters("\nabc\u0007def")).isTrue();
  }
}
