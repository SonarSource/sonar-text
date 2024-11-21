/*
 * SonarQube Text Plugin
 * Copyright (C) 2021-2024 SonarSource SA
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
package org.sonar.plugins.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.plugins.common.BinaryFileUtils.hasNonTextCharacters;

class BinaryFileUtilsTest {

  @Test
  void has_non_text_characters() {
    assertThat(hasNonTextCharacters("")).isFalse();
    assertThat(hasNonTextCharacters("abc\tdef\r\n")).isFalse();
    assertThat(hasNonTextCharacters("abc\tdef\r\n\u00FF")).isFalse();
    assertThat(hasNonTextCharacters("\u0001abc")).isTrue();
    assertThat(hasNonTextCharacters("abc\u0007def")).isTrue();
    assertThat(hasNonTextCharacters("\nabc\u0007def")).isTrue();
    assertThat(hasNonTextCharacters("abc\u001B[1m;def\u0008ghi\0")).isFalse();
  }

}
