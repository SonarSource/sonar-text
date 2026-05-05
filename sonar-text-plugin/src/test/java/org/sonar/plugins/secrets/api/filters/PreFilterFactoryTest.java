/*
 * SonarQube Text Plugin
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.plugins.secrets.api.filters;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.plugins.common.InputFileContext;
import org.sonar.plugins.secrets.api.MessageFormatter;
import org.sonar.plugins.secrets.api.SpecificationConfiguration;
import org.sonar.plugins.secrets.configuration.model.Selectivity;
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

    var filter = PreFilterFactory.createFilter(detection.getPre(), Selectivity.SPECIFIC, new SpecificationConfiguration(false), true);

    InputFileContext ctx = mock(InputFileContext.class);
    when(ctx.getInputFile()).thenReturn(new TestInputFileBuilder("myProject", filename).build());
    when(ctx.getFileSystem()).thenReturn(new DefaultFileSystem(Path.of(".")));
    when(ctx.lines()).thenReturn(List.of());

    assertThat(filter.apply(ctx).passed()).isEqualTo(shouldMatch);
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
  @MethodSource("org.sonar.plugins.secrets.api.AutomaticTestFileFilterTest#inputsForTestingMainScopeAndSonarTest")
  void shouldAcceptMainScopeWhenSonarTestIsNotSet(String filePath, boolean shouldMatch) {
    testPredicateWithScopeAndConfiguration(filePath, "/base/directory/", SpecificationConfiguration.AUTO_TEST_FILE_DETECTION_ENABLED, shouldMatch);
  }

  @ParameterizedTest
  @MethodSource("org.sonar.plugins.secrets.api.AutomaticTestFileFilterTest#inputsForTestingMainScopeAndSonarTest")
  void shouldAcceptMainScopeWhenSonarTestIsNotSetAndBaseDirectoryContainsTest(String filePath, boolean shouldMatch) {
    testPredicateWithScopeAndConfiguration(filePath, "/base/directory/with/test/in/it/", SpecificationConfiguration.AUTO_TEST_FILE_DETECTION_ENABLED, shouldMatch);
  }

  @ParameterizedTest
  @MethodSource("org.sonar.plugins.secrets.api.AutomaticTestFileFilterTest#inputsForTestingMainScopeAndSonarTest")
  void shouldTestMainScopeWhenSonarTestIsSet(String filePath, boolean ignored) {
    testPredicateWithScopeAndConfiguration(filePath, "/base/directory/", SpecificationConfiguration.AUTO_TEST_FILE_DETECTION_DISABLED, true);
  }

  private void testPredicateWithScopeAndConfiguration(String filePath, String baseDir, SpecificationConfiguration configuration, boolean expected) {
    var preModule = new PreModule();

    String projectKey = "myProject";

    var inputFile = spy(new TestInputFileBuilder(projectKey, filePath).build());
    URI uri = URI.create("file:" + baseDir + projectKey + "/" + filePath);
    when(inputFile.uri()).thenReturn(uri);

    DefaultFileSystem fileSystem = new DefaultFileSystem(Path.of(baseDir));

    var ctx = mock(InputFileContext.class);
    when(ctx.getInputFile()).thenReturn(inputFile);
    when(ctx.getFileSystem()).thenReturn(fileSystem);

    var filter = PreFilterFactory.createFilter(preModule, Selectivity.SPECIFIC, configuration, true);

    assertThat(filter.apply(ctx).passed())
      .withFailMessage("Input file uri: " + inputFile.uri().getPath())
      .isEqualTo(expected);
  }

  @ParameterizedTest
  @MethodSource("org.sonar.plugins.secrets.api.AutomaticTestFileFilterTest#inputsForTestingMainScopeWithWrongBaseDirectory")
  void shouldTestMainScopeWhenFileSystemBaseDirPathTypeIsWrong(String filePath, boolean shouldMatch) {
    var inputFile = spy(new TestInputFileBuilder("myProject", filePath).build());
    URI uri = URI.create("file:/base/directory/myProject/" + filePath);
    when(inputFile.uri()).thenReturn(uri);

    DefaultFileSystem fileSystem = new DefaultFileSystem(Path.of("./wrong/base/directory/path/type"));

    var ctx = mock(InputFileContext.class);
    when(ctx.getInputFile()).thenReturn(inputFile);
    when(ctx.getFileSystem()).thenReturn(fileSystem);

    var filter = PreFilterFactory.createFilter(new PreModule(), Selectivity.SPECIFIC, SpecificationConfiguration.AUTO_TEST_FILE_DETECTION_ENABLED, true);
    assertThat(filter.apply(ctx).passed()).isEqualTo(shouldMatch);
  }

  @ParameterizedTest
  @ValueSource(strings = {"true", "false"})
  void shouldNotAcceptTestFiles(boolean automaticTestDetectionEnabled) {
    var ctx = fileContext("file.txt", "/base/directory/", InputFile.Type.TEST);
    var filter = PreFilterFactory.createFilter(new PreModule(), Selectivity.SPECIFIC, new SpecificationConfiguration(automaticTestDetectionEnabled), true);
    assertThat(filter.apply(ctx).passed()).isFalse();
  }

  @Test
  void createFilterReturnsAcceptedForNonTestMainFile() {
    var ctx = fileContext("src/file.txt", "/base/directory/", InputFile.Type.MAIN);
    var filter = PreFilterFactory.createFilter(new PreModule(), Selectivity.SPECIFIC, new SpecificationConfiguration(true), true);
    assertThat(filter.apply(ctx)).isEqualTo(FilterOutcome.ACCEPTED);
  }

  @Test
  void createFilterReturnsRejectedForAutomaticTestFileWhenFilterEnabled() {
    var ctx = fileContext("test_secrets.txt", "/base/directory/", InputFile.Type.MAIN);
    var filter = PreFilterFactory.createFilter(new PreModule(), Selectivity.SPECIFIC, new SpecificationConfiguration(true), true);
    assertThat(filter.apply(ctx)).isEqualTo(FilterOutcome.REJECTED);
  }

  @Test
  void createFilterReturnsPassedWithSkippedForAutomaticTestFileWhenFilterDisabled() {
    var ctx = fileContext("test_secrets.txt", "/base/directory/", InputFile.Type.MAIN);
    var config = new SpecificationConfiguration(true, Set.of(SkippedFilter.TEST_FILES_FILTER), MessageFormatter.RULE_MESSAGE);
    var filter = PreFilterFactory.createFilter(new PreModule(), Selectivity.SPECIFIC, config, true);
    var outcome = filter.apply(ctx);
    assertThat(outcome.passed()).isTrue();
    assertThat(outcome.skipped()).containsExactly(SkippedFilter.TEST_FILES_FILTER);
  }

  @Test
  void createFilterReturnsAcceptedForAutomaticTestFileWhenAutomaticDetectionIsOff() {
    var ctx = fileContext("test_secrets.txt", "/base/directory/", InputFile.Type.MAIN);
    // When automatic test detection is off (user configured sonar.tests), the automatic filter is never
    // applied — skipping it via --disable-test-file-detection has no effect.
    var filter = PreFilterFactory.createFilter(new PreModule(), Selectivity.SPECIFIC, new SpecificationConfiguration(false), true);
    assertThat(filter.apply(ctx)).isEqualTo(FilterOutcome.ACCEPTED);
  }

  @Test
  void createFilterRejectsExplicitTestFileTypeEvenWhenTestFilesFilterDisabled() {
    var ctx = fileContext("file.txt", "/base/directory/", InputFile.Type.TEST);
    // When a file is explicitly classified as Type.TEST (user configured sonar.tests), it's rejected before
    // the automatic filter runs — skipping that filter has no effect.
    var config = new SpecificationConfiguration(true, Set.of(SkippedFilter.TEST_FILES_FILTER), MessageFormatter.RULE_MESSAGE);
    var filter = PreFilterFactory.createFilter(new PreModule(), Selectivity.SPECIFIC, config, true);
    assertThat(filter.apply(ctx)).isEqualTo(FilterOutcome.REJECTED);
  }

  private static InputFileContext fileContext(String filePath, String baseDir, InputFile.Type type) {
    String projectKey = "myProject";
    var inputFile = spy(new TestInputFileBuilder(projectKey, filePath).setType(type).build());
    when(inputFile.uri()).thenReturn(URI.create("file:" + baseDir + projectKey + "/" + filePath));
    var ctx = mock(InputFileContext.class);
    when(ctx.getInputFile()).thenReturn(inputFile);
    when(ctx.getFileSystem()).thenReturn(new DefaultFileSystem(Path.of(baseDir)));
    when(ctx.lines()).thenReturn(List.of());
    return ctx;
  }

  @ParameterizedTest
  @MethodSource
  void matchesBasePredicateBySelectivityWithInputFileLanguage(Selectivity selectivity, String language, boolean shouldMatch) {
    InputFileContext ctx = mock(InputFileContext.class);
    when(ctx.getInputFile()).thenReturn(mock(InputFile.class));
    when(ctx.getInputFile().language()).thenReturn(language);

    var selectivityPredicate = PreFilterFactory.appendSelectivityPredicate(context -> true, selectivity);
    assertThat(selectivityPredicate.test(ctx)).isEqualTo(shouldMatch);
  }

  static Stream<Arguments> matchesBasePredicateBySelectivityWithInputFileLanguage() {
    return Stream.of(
      Arguments.of(Selectivity.SPECIFIC, null, true),
      Arguments.of(Selectivity.PROVIDER_GENERIC, null, true),
      Arguments.of(Selectivity.ANALYZER_GENERIC, null, true),

      Arguments.of(Selectivity.SPECIFIC, "myLanguage", true),
      Arguments.of(Selectivity.PROVIDER_GENERIC, "myLanguage", true),
      Arguments.of(Selectivity.ANALYZER_GENERIC, "myLanguage", false));
  }
}
