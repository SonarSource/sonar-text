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
import javax.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.InputFile;

import static org.assertj.core.api.Assertions.assertThat;

class CombinedFilePredicateTest {

  private static final FilePredicate LANGUAGE_FILE_PREDICATE = inputFile -> inputFile.language() != null;

  @Test
  void anyShouldBeTrueWhenAnyPredicateIsTrue() {
    FilePredicate predicate = CombinedFilePredicate.any(new TextFilePredicate("txt"), LANGUAGE_FILE_PREDICATE);
    assertThat(predicate.apply(inputFile("ok.txt", "java"))).isTrue();
    assertThat(predicate.apply(inputFile("ok.txt", null))).isTrue();
    assertThat(predicate.apply(inputFile("ko.exe", "java"))).isTrue();
    assertThat(predicate.apply(inputFile("ko.exe", null))).isFalse();
  }

  private static InputFile inputFile(String filename, @Nullable String language) {
    return TestUtils.inputFile(Path.of(filename), "// empty content", language);
  }
}
