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
package org.sonar.plugins.secrets.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.plugins.common.InputFileContext;
import org.sonar.plugins.secrets.configuration.model.matching.Detection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PreFilterFactoryTest {
  private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory());

  @ParameterizedTest
  @CsvSource({
    ".doc, file.doc, true",
    "doc, file.doc, true",
    ".doc, file.cpp, false",
    "pp, file.cpp, false",
    "'', file.cpp, false",
  })
  void testMatchesExt(String ext, String filename, boolean shouldMatch) {
    InputFileContext ctx = mock(InputFileContext.class);
    when(ctx.getInputFile()).thenReturn(mock(InputFile.class));
    when(ctx.getInputFile().filename()).thenReturn(filename);
    when(ctx.getFileSystem()).thenReturn(new DefaultFileSystem(Path.of(".")));
    assertThat(PreFilterFactory.matchesExt(ext, ctx)).isEqualTo(shouldMatch);
  }

  @ParameterizedTest
  @CsvSource({
    "**/src/*.cpp, project/src/file.cpp, true",
    "**/src/*.cpp, project/src/file.java, false",
    "**/src/**/*.cpp, project/src/main/file.cpp, true",
    "**/src/*, project/src/file.cpp, true",
    "**/src/*, project/resources/file.json, false",
    "**/src/**, project/src/main/file.cpp, true",
    "**/file.cpp, project/src/file.cpp, true",
    "**/file.cpp, project/src/file.cpp, true",
    "'', project/src/file.cpp, false",
  })
  void testMatchesPath(String pathPattern, String filePath, boolean shouldMatch) throws URISyntaxException {
    InputFileContext ctx = mock(InputFileContext.class);
    when(ctx.getInputFile()).thenReturn(new TestInputFileBuilder(
      "testProject", filePath).build());
    when(ctx.getFileSystem()).thenReturn(new DefaultFileSystem(Path.of(".")));
    assertThat(PreFilterFactory.matchesPath(pathPattern, ctx)).isEqualTo(shouldMatch);
  }

  @ParameterizedTest
  @CsvSource({
    "secretstring, secretstring, true",
    "secretstring, not-so-secret-string, false",
    "'', secretstring, false",
  })
  void testMatchesContent(String contentPattern, String fileContent, boolean shouldMatch) {
    InputFileContext ctx = mock(InputFileContext.class);
    when(ctx.lines()).thenReturn(List.of(fileContent));
    assertThat(PreFilterFactory.matchesContent(contentPattern, ctx)).isEqualTo(shouldMatch);
  }

  @ParameterizedTest
  @MethodSource("inputs")
  void testFiltersFromYamlFragments(String input, String filename, boolean shouldMatch) throws IOException {
    Detection detection = MAPPER.readValue(input, Detection.class);

    Predicate<InputFileContext> predicate = PreFilterFactory.createPredicate(detection.getPre());

    InputFileContext ctx = mock(InputFileContext.class);
    when(ctx.getInputFile()).thenReturn(new TestInputFileBuilder(
      "testProject", filename).build());
    when(ctx.getFileSystem()).thenReturn(new DefaultFileSystem(Path.of(".")));
    when(ctx.lines()).thenReturn(List.of());

    assertThat(predicate.test(ctx)).isEqualTo(shouldMatch);
  }

  static Stream<Arguments> inputs() {
    return Stream.of(
      Arguments.of("pre:", ".env", true),
      Arguments.of(
        "pre:\n" +
          "  include:\n" +
          "    paths:\n" +
          "      - \"**/.env\"",
        ".env",
        true),
      Arguments.of(
        "pre:\n" +
          "  reject:\n" +
          "    paths:\n" +
          "      - \"**/.env\"",
        ".env",
        false),
      Arguments.of(
        "pre:\n" +
          "  include:\n" +
          "    paths:\n" +
          "      - \"**/.env\"\n" +
          "  reject:\n" +
          "    paths:\n" +
          "      - \"**/.env\"",
        ".env",
        false),
      Arguments.of(
        "pre:\n" +
          "  include:\n" +
          "    ext:\n" +
          "      - \".java\"",
        "Foo.java",
        true),
      Arguments.of(
        "pre:\n" +
          "  include:\n" +
          "    ext:\n" +
          "      - \".java\"",
        "Foo.cpp",
        false),
      Arguments.of(
        "pre:\n" +
          "  reject:\n" +
          "    ext:\n" +
          "      - \".class\"",
        "Foo.class",
        false));
  }
}
