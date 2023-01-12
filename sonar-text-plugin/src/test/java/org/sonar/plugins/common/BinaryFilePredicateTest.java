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
    assertThat(predicate.hasBinaryFileName("foo.foo")).isTrue();
    assertThat(predicate.hasBinaryFileName("foo.exe")).isTrue();
    assertThat(predicate.hasBinaryFileName("foo.EXE")).isTrue();
    assertThat(predicate.hasBinaryFileName("foo.UNKNOWN")).isFalse();
    assertThat(predicate.hasBinaryFileName("foo.txt")).isFalse();
    assertThat(predicate.hasBinaryFileName("boom.foo.bar")).isTrue();
    assertThat(predicate.hasBinaryFileName("boom.foo.other")).isFalse();
    assertThat(predicate.hasBinaryFileName("boom_suffix")).isFalse();
    assertThat(predicate.hasBinaryFileName("boom_my_suffix")).isTrue();
    assertThat(predicate.hasBinaryFileName("_")).isFalse();
    assertThat(predicate.hasBinaryFileName(".")).isTrue();
    assertThat(predicate.hasBinaryFileName("")).isFalse();
    assertThat(predicate.hasBinaryFileName("cacerts")).isTrue();
    assertThat(predicate.hasBinaryFileName("ec54a0d82c5938c")).isFalse();
    assertThat(predicate.hasBinaryFileName("EC54A0D82C5938C")).isFalse();
    assertThat(predicate.hasBinaryFileName("9f17e505386ec54a0d82c5938ca15805")).isTrue();
    assertThat(predicate.hasBinaryFileName("9F17E505386EC54A0D82C5938CA15805")).isTrue();
    assertThat(predicate.hasBinaryFileName("abac0000111122222233333444455555")).isFalse();
    assertThat(predicate.hasBinaryFileName("Boum123Boum456Boum789Boum123Boum")).isFalse();
    assertThat(predicate.hasBinaryFileName("9f17e505386ec54a0d82c5938ca15805.txt")).isFalse();
    assertThat(predicate.hasBinaryFileName("71c686d48275f9b552946aedf50d67e218064cbd")).isTrue();
    assertThat(predicate.hasBinaryFileName("9dc59c1853a762fb0fe420b0e18b7861b0045f291f8dd75ab9e9d578a31e729a")).isTrue();
    assertThat(predicate.hasBinaryFileName("faccb33a9627c7269e7ce4e4a37beb2809410a90612977b61944b1680411f39eb2b773adf539d320c3b48c1a0ddfc9b01fc0673a1d893cdb30878b7432f0ebc6")).isTrue();
    predicate.addBinaryFileExtension("txt");
    assertThat(predicate.hasBinaryFileName("foo.txt")).isTrue();
  }

  @Test
  void extensions() {
    assertThat(BinaryFilePredicate.extension("")).isNull();
    assertThat(BinaryFilePredicate.extension("foo.txt")).isEqualTo("txt");
    assertThat(BinaryFilePredicate.extension("foo")).isNull();
    assertThat(BinaryFilePredicate.extension(".txt")).isEqualTo("txt");
    assertThat(BinaryFilePredicate.extension("foo.bar.c")).isEqualTo("c");
    assertThat(BinaryFilePredicate.extension("...c")).isEqualTo("c");
    assertThat(BinaryFilePredicate.extension("foo.")).isNull();
    assertThat(BinaryFilePredicate.extension("foo.bar.")).isNull();;
  }

}
