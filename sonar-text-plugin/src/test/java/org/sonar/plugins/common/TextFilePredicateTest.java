/*
 * SonarQube Text Plugin
 * Copyright (C) 2021-2023 SonarSource SA
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

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.sonar.api.batch.fs.InputFile;

import static org.assertj.core.api.Assertions.assertThat;

class TextFilePredicateTest {

  @Test
  void applyShouldReturnTrue() {
    TextFilePredicate predicate = new TextFilePredicate("txt", "csv");
    assertThat(predicate.apply(inputFile("foo.txt"))).isTrue();
    assertThat(predicate.apply(inputFile("foo.csv"))).isTrue();
    assertThat(predicate.apply(inputFile("foo.CSV"))).isTrue();
    assertThat(predicate.apply(inputFile("boom.foo.txt"))).isTrue();
  }

  @Test
  void applyShouldReturnFalse() {
    TextFilePredicate predicate = new TextFilePredicate("txt", "csv");
    assertThat(predicate.apply(inputFile("foo.exe"))).isFalse();
    assertThat(predicate.apply(inputFile("foo.UNKNOWN"))).isFalse();
    assertThat(predicate.apply(inputFile("boom.foo.other"))).isFalse();
    assertThat(predicate.apply(inputFile("boom_txt"))).isFalse();
    assertThat(predicate.apply(inputFile("_"))).isFalse();
    assertThat(predicate.apply(inputFile("foo."))).isFalse();
    assertThat(predicate.apply(inputFile("txt."))).isFalse();
    assertThat(predicate.apply(inputFile("bar."))).isFalse();
    assertThat(predicate.apply(inputFile(""))).isFalse();
  }

  private static InputFile inputFile(String filename) {
    return TestUtils.inputFile(Path.of(filename), "// empty content");
  }

}
