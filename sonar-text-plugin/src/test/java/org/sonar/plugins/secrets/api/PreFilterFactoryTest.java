/*
 * SonarQube Text Plugin
 * Copyright (C) 2021-2024 SonarSource SA
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
import static org.junit.jupiter.params.provider.Arguments.arguments;
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
  void testMatchesPath(String pathPattern, String filePath, boolean shouldMatch) {
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

    Predicate<InputFileContext> predicate = PreFilterFactory.createPredicate(detection.getPre(), new SpecificationConfiguration("src/tests"));

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
  @MethodSource("inputsForTestingMainScopeAndSonarTest")
  void shouldTestMainScopeWhenSonarTestIsNotSet(String filePath, boolean shouldMatch) {
    var preModule = new PreModule();
    preModule.setScopes(List.of(RuleScope.MAIN));
    var ctx = mock(InputFileContext.class);
    when(ctx.getInputFile()).thenReturn(new TestInputFileBuilder(
      "testProject", filePath).build());

    var predicate = PreFilterFactory.createPredicate(preModule, SpecificationConfiguration.NO_CONFIGURATION);

    assertThat(predicate.test(ctx)).isEqualTo(shouldMatch);
  }

  @ParameterizedTest
  @MethodSource("inputsForTestingMainScopeAndSonarTest")
  void shouldTestMainScopeWhenSonarTestIsSet(String filePath, boolean ignored) {
    var preModule = new PreModule();
    preModule.setScopes(List.of(RuleScope.MAIN));
    var ctx = mock(InputFileContext.class);
    when(ctx.getInputFile()).thenReturn(new TestInputFileBuilder(
      "testProject", filePath).build());

    var predicate = PreFilterFactory.createPredicate(preModule, new SpecificationConfiguration("/src/tests"));

    assertThat(predicate.test(ctx)).isTrue();
  }

  @ParameterizedTest
  @MethodSource("inputsForTestingMainScopeAndSonarTest")
  void shouldTestMainScopeWhenSonarTestIsNotSetAndScopeTest(String filePath, boolean ignored) {
    var preModule = new PreModule();
    preModule.setScopes(List.of(RuleScope.TEST));
    var ctx = mock(InputFileContext.class);
    when(ctx.getInputFile()).thenReturn(new TestInputFileBuilder(
      "testProject", filePath).build());

    var predicate = PreFilterFactory.createPredicate(preModule, SpecificationConfiguration.NO_CONFIGURATION);

    assertThat(predicate.test(ctx)).isFalse();
  }

  static Stream<Arguments> inputsForTestingMainScopeAndSonarTest() {
    return Stream.of(
      arguments("testutils.go", false),
      arguments("Testutils.go", false),
      arguments("test_example.py", false),
      arguments("Someutils.go", true),
      arguments("src/main/ExampleFilterTest.java", false),
      arguments("src/main/examplefiltertest.java", false),
      arguments("src/main/ExampleFilterTestt.java", true),
      arguments("docker-compose-api-test.yaml", false),
      arguments("conftest.py", false),
      arguments("example_test.go", false),
      arguments("appsettings.Test.json", false),
      arguments("docker-compose-tests.yaml", false),
      arguments("docker-compose-Tests.yaml", false),
      arguments("docker-compose-Testss.yaml", true),
      arguments("doc/examples/Example.java", false),
      arguments("doc/examples/example.go", false),
      arguments("documentation/examples/example.go", true),
      arguments("examples/docs/example.go", false),
      arguments("examples/docs/example.go", false),
      arguments("examples/docs/src/main/example.go", false),
      arguments("src/test/java/org/sonar/appsettings.json", false),
      arguments("src/Test/java/org/sonar/appsettings.json", false),
      arguments("tests/appsettings.json", false),
      arguments("a/b/c/tests/appsettings.json", false),
      arguments("a/b/c/doc", true), // doc is a filename here
      arguments("libraries/kotlin.test/js/it/js/jest-reporter.js", false),
      arguments("libraries/kotlin.Test/js/it/js/jest-reporter.js", false),
      arguments("native/native.tests/driver/testData/driver0.kt", false),
      arguments("native/native.Tests/driver/testData/driver0.kt", false),
      arguments("latestnews.html", true),
      arguments("abc/latestnews.html", true),
      arguments("contestresults.js", true),
      arguments("Protester.java", true));
  }
}
