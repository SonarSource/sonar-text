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

class BinaryFilePredicateTest {

  @Test
  void all_values() {
    BinaryFilePredicate predicate = new BinaryFilePredicate(".foo", ".foo.bar", "_my_suffix", "", ".");
    assertThat(predicate.isBinaryFile("foo.foo")).isTrue();
    assertThat(predicate.isBinaryFile("foo.exe")).isTrue();
    assertThat(predicate.isBinaryFile("foo.EXE")).isTrue();
    assertThat(predicate.isBinaryFile("foo.UNKNOWN")).isFalse();
    assertThat(predicate.isBinaryFile("foo.txt")).isFalse();
    assertThat(predicate.isBinaryFile("boom.foo.bar")).isTrue();
    assertThat(predicate.isBinaryFile("boom.foo.other")).isFalse();
    assertThat(predicate.isBinaryFile("boom_suffix")).isFalse();
    assertThat(predicate.isBinaryFile("boom_my_suffix")).isTrue();
    assertThat(predicate.isBinaryFile("_")).isFalse();
    assertThat(predicate.isBinaryFile(".")).isTrue();
    assertThat(predicate.isBinaryFile("")).isFalse();
    predicate.addBinaryFileExtension("txt");
    assertThat(predicate.isBinaryFile("foo.txt")).isTrue();
  }

  @Test
  void extensions() {
    assertThat(BinaryFilePredicate.extension("")).isEmpty();
    assertThat(BinaryFilePredicate.extension("foo.txt")).isEqualTo("txt");
    assertThat(BinaryFilePredicate.extension("foo")).isEqualTo("foo");
    assertThat(BinaryFilePredicate.extension(".txt")).isEqualTo("txt");
    assertThat(BinaryFilePredicate.extension("foo.bar.c")).isEqualTo("c");
    assertThat(BinaryFilePredicate.extension("...c")).isEqualTo("c");
    assertThat(BinaryFilePredicate.extension("foo.")).isEqualTo("foo.");
    assertThat(BinaryFilePredicate.extension("foo.bar.")).isEqualTo("foo.bar.");
  }

}
