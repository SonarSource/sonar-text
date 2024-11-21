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

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.sonar.api.batch.fs.InputFile;

import static org.assertj.core.api.Assertions.assertThat;

class NotBinaryFilePredicateTest {

  @Test
  void all_values() {
    NotBinaryFilePredicate predicate = new NotBinaryFilePredicate(".foo", ".foo.bar", "_my_suffix", "", "oo.");
    assertThat(predicate.apply(inputFile("foo.foo"))).isFalse();
    assertThat(predicate.apply(inputFile("foo.exe"))).isFalse();
    assertThat(predicate.apply(inputFile("foo.EXE"))).isFalse();
    assertThat(predicate.apply(inputFile("foo.UNKNOWN"))).isTrue();
    assertThat(predicate.apply(inputFile("foo.txt"))).isTrue();
    assertThat(predicate.apply(inputFile("boom.foo.bar"))).isFalse();
    assertThat(predicate.apply(inputFile("boom.foo.other"))).isTrue();
    assertThat(predicate.apply(inputFile("boom_suffix"))).isTrue();
    assertThat(predicate.apply(inputFile("boom_my_suffix"))).isFalse();
    assertThat(predicate.apply(inputFile("_"))).isTrue();
    assertThat(predicate.apply(inputFile("foo."))).isFalse();
    assertThat(predicate.apply(inputFile("bar."))).isTrue();
    assertThat(predicate.apply(inputFile(""))).isTrue();
    assertThat(predicate.apply(inputFile("cacerts"))).isFalse();
    assertThat(predicate.apply(inputFile("ec54a0d82c5938c"))).isTrue();
    assertThat(predicate.apply(inputFile("EC54A0D82C5938C"))).isTrue();
    assertThat(predicate.apply(inputFile("9f17e505386ec54a0d82c5938ca15805"))).isFalse();
    assertThat(predicate.apply(inputFile("9F17E505386EC54A0D82C5938CA15805"))).isFalse();
    assertThat(predicate.apply(inputFile("abac0000111122222233333444455555"))).isTrue();
    assertThat(predicate.apply(inputFile("Boum123Boum456Boum789Boum123Boum"))).isTrue();
    assertThat(predicate.apply(inputFile("9f17e505386ec54a0d82c5938ca15805.txt"))).isTrue();
    assertThat(predicate.apply(inputFile("71c686d48275f9b552946aedf50d67e218064cbd"))).isFalse();
    assertThat(predicate.apply(inputFile("9dc59c1853a762fb0fe420b0e18b7861b0045f291f8dd75ab9e9d578a31e729a"))).isFalse();
    assertThat(predicate.apply(inputFile("faccb33a9627c7269e7ce4e4a37beb2809410a90612977b61944b1680411f39eb2b773adf539d320c3b48c1a0ddfc9b01fc0673a1d893cdb30878b7432f0ebc6")))
      .isFalse();
    predicate.addBinaryFileExtension("txt");
    assertThat(predicate.apply(inputFile("foo.txt"))).isFalse();
  }

  private static InputFile inputFile(String filename) {
    return TestUtils.inputFile(Path.of(filename), "// empty content");
  }

  @Test
  void extensions() {
    assertThat(NotBinaryFilePredicate.extension("")).isNull();
    assertThat(NotBinaryFilePredicate.extension("foo.txt")).isEqualTo("txt");
    assertThat(NotBinaryFilePredicate.extension("foo")).isNull();
    assertThat(NotBinaryFilePredicate.extension(".txt")).isEqualTo("txt");
    assertThat(NotBinaryFilePredicate.extension("foo.bar.c")).isEqualTo("c");
    assertThat(NotBinaryFilePredicate.extension("...c")).isEqualTo("c");
    assertThat(NotBinaryFilePredicate.extension("foo.")).isNull();
    assertThat(NotBinaryFilePredicate.extension("foo.bar.")).isNull();
    ;
  }

}
