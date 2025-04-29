/*
 * SonarQube Text Plugin
 * Copyright (C) 2021-2025 SonarSource SA
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
package org.sonar.plugins.secrets.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.net.URI;
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
import org.sonar.plugins.secrets.configuration.model.RuleScope;
import org.sonar.plugins.secrets.configuration.model.matching.Detection;
import org.sonar.plugins.secrets.configuration.model.matching.filter.PreModule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
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
  void testMatchesPath(String pathPattern, String filePath, boolean shouldMatch) {
    InputFileContext ctx = mock(InputFileContext.class);
    when(ctx.getInputFile()).thenReturn(new TestInputFileBuilder("myProject", filePath).build());
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

    Predicate<InputFileContext> predicate = PreFilterFactory.createPredicate(detection.getPre(), new SpecificationConfiguration(false));

    InputFileContext ctx = mock(InputFileContext.class);
    when(ctx.getInputFile()).thenReturn(new TestInputFileBuilder("myProject", filename).build());
    when(ctx.getFileSystem()).thenReturn(new DefaultFileSystem(Path.of(".")));
    when(ctx.lines()).thenReturn(List.of());

    assertThat(predicate.test(ctx)).isEqualTo(shouldMatch);
  }

  static Stream<Arguments> inputs() {
    return Stream.of(
      Arguments.of("pre:", ".env", true),
      Arguments.of(
        """
          pre:
            include:
              paths:
                - "**/.env\"""",
        ".env",
        true),
      Arguments.of(
        """
          pre:
            reject:
              paths:
                - "**/.env\"""",
        ".env",
        false),
      Arguments.of(
        """
          pre:
            include:
              paths:
                - "**/.env"
            reject:
              paths:
                - "**/.env\"""",
        ".env",
        false),
      Arguments.of(
        """
          pre:
            include:
              ext:
                - ".java\"""",
        "Foo.java",
        true),
      Arguments.of(
        """
          pre:
            include:
              ext:
                - ".java\"""",
        "Foo.cpp",
        false),
      Arguments.of(
        """
          pre:
            reject:
              ext:
                - ".class\"""",
        "Foo.class",
        false));
  }

  @ParameterizedTest
  @MethodSource("org.sonar.plugins.secrets.api.ScopeBasedFileFilterTest#inputsForTestingMainScopeAndSonarTest")
  void shouldTestMainScopeWhenSonarTestIsNotSet(String filePath, boolean shouldMatch) {
    testPredicateWithScopeAndConfiguration(filePath, "/base/directory/", RuleScope.MAIN, SpecificationConfiguration.AUTO_TEST_FILE_DETECTION_ENABLED, shouldMatch);
  }

  @ParameterizedTest
  @MethodSource("org.sonar.plugins.secrets.api.ScopeBasedFileFilterTest#inputsForTestingMainScopeAndSonarTest")
  void shouldTestMainScopeWhenSonarTestIsNotSetAndBaseDirectoryContainsTest(String filePath, boolean shouldMatch) {
    testPredicateWithScopeAndConfiguration(filePath, "/base/directory/with/test/in/it/", RuleScope.MAIN, SpecificationConfiguration.AUTO_TEST_FILE_DETECTION_ENABLED, shouldMatch);
  }

  @ParameterizedTest
  @MethodSource("org.sonar.plugins.secrets.api.ScopeBasedFileFilterTest#inputsForTestingMainScopeAndSonarTest")
  void shouldTestMainScopeWhenSonarTestIsSet(String filePath, boolean ignored) {
    testPredicateWithScopeAndConfiguration(filePath, "/base/directory/", RuleScope.MAIN, SpecificationConfiguration.AUTO_TEST_FILE_DETECTION_DISABLED, true);
  }

  @ParameterizedTest
  @MethodSource("org.sonar.plugins.secrets.api.ScopeBasedFileFilterTest#inputsForTestingMainScopeAndSonarTest")
  void shouldTestMainScopeWhenSonarTestIsNotSetAndScopeTest(String filePath, boolean ignored) {
    testPredicateWithScopeAndConfiguration(filePath, "/base/directory/", RuleScope.TEST, SpecificationConfiguration.AUTO_TEST_FILE_DETECTION_ENABLED, false);
  }

  private void testPredicateWithScopeAndConfiguration(String filePath, String baseDir, RuleScope scope, SpecificationConfiguration configuration, boolean expected) {
    var preModule = new PreModule();
    preModule.setScopes(List.of(scope));

    String projectKey = "myProject";

    var inputFile = spy(new TestInputFileBuilder(projectKey, filePath).build());
    URI uri = URI.create("file:" + baseDir + projectKey + "/" + filePath);
    when(inputFile.uri()).thenReturn(uri);

    DefaultFileSystem fileSystem = new DefaultFileSystem(Path.of(baseDir));

    var ctx = mock(InputFileContext.class);
    when(ctx.getInputFile()).thenReturn(inputFile);
    when(ctx.getFileSystem()).thenReturn(fileSystem);

    var predicate = PreFilterFactory.createPredicate(preModule, configuration);

    assertThat(predicate.test(ctx))
      .withFailMessage("Input file uri: " + inputFile.uri().getPath())
      .isEqualTo(expected);
  }

  @ParameterizedTest
  @MethodSource("org.sonar.plugins.secrets.api.ScopeBasedFileFilterTest#inputsForTestingMainScopeWithWrongBaseDirectory")
  void shouldTestMainScopeWhenFileSystemBaseDirPathTypeIsWrong(String filePath, boolean shouldMatch) {
    var preModule = new PreModule();
    preModule.setScopes(List.of(RuleScope.MAIN));

    var inputFile = spy(new TestInputFileBuilder("myProject", filePath).build());
    URI uri = URI.create("file:/base/directory/myProject/" + filePath);
    when(inputFile.uri()).thenReturn(uri);

    DefaultFileSystem fileSystem = new DefaultFileSystem(Path.of("./wrong/base/directory/path/type"));

    var ctx = mock(InputFileContext.class);
    when(ctx.getInputFile()).thenReturn(inputFile);
    when(ctx.getFileSystem()).thenReturn(fileSystem);

    var predicate = PreFilterFactory.createPredicate(preModule, SpecificationConfiguration.AUTO_TEST_FILE_DETECTION_ENABLED);
    assertThat(predicate.test(ctx)).isEqualTo(shouldMatch);
  }
}
