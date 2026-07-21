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
package org.sonar.plugins.common.predicates;

import com.sonarsource.scanner.engine.sensor.test.fixtures.SensorContextTester;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.StreamSupport;
import javax.annotation.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.event.Level;
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonar.plugins.common.TestUtils;
import org.sonar.plugins.common.git.GitService;
import org.sonar.plugins.common.measures.TelemetryReporter;
import org.sonar.plugins.common.warnings.AnalysisWarningsWrapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.plugins.common.TestUtils.SONARLINT_RUNTIME;
import static org.sonar.plugins.common.TestUtils.SONARQUBE_RUNTIME_WITHOUT_HIDDEN_FILES_SUPPORT;
import static org.sonar.plugins.common.TestUtils.inputFile;
import static org.sonar.plugins.common.predicates.TextAndSecretsPredicates.INCLUSIONS_ACTIVATION_KEY;
import static org.sonar.plugins.common.predicates.TextAndSecretsPredicates.TEXT_INCLUSIONS_DEFAULT_VALUE;
import static org.sonar.plugins.common.predicates.TextAndSecretsPredicates.TEXT_INCLUSIONS_KEY;

class TextAndSecretsPredicatesTest {

  @RegisterExtension
  LogTesterJUnit5 logTester = new LogTesterJUnit5();

  private TextAndSecretsPredicates textAndSecretsPredicates;
  private SensorContextTester sensorContext;
  private TelemetryReporter telemetryReporter;
  private GitService gitService;

  @BeforeEach
  void setUp() {
    sensorContext = TestUtils.sensorContext(new String[] {});
    var durationStatistics = TestUtils.mockDurationStatistics();
    telemetryReporter = spy(new TelemetryReporter(sensorContext));
    var analysisWarningsWrapper = spy(AnalysisWarningsWrapper.class);
    gitService = mock(GitService.class);
    when(gitService.retrieveDirtyFileNames()).thenReturn(new GitService.DirtyFileNamesResult(true, Set.of()));
    textAndSecretsPredicates = new TextAndSecretsPredicates(sensorContext, durationStatistics, telemetryReporter, gitService, analysisWarningsWrapper);
  }

  @ParameterizedTest
  @ValueSource(strings = {
    "file.md",
    "file.adoc",
    "file.html",
    "file.example",
    "file.sample",
    "file.template",
    "file.dist",
    "file.mdx",
    ".env.dist",
    "foo.bar.md",

    // binary files
    "file.class",

    // a
    "file.csv",

    // no language assigned
    "file.java",

    "fileWithoutExtension"
  })
  void shouldNotMatchFileWithoutLanguage(String fileName) {
    InputFile inputFile = inputFile(Path.of(fileName), "");
    var actualMatchedFiles = textAndSecretsPredicateMatchingFiles(inputFile);
    assertThat(actualMatchedFiles).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(strings = {
    // path inclusions
    "md",
    "adoc",
    "html",
    "example",
    "sample",
    "template",
    "dist",
    "mdx",
    "env.dist",
    "bar.md",
  })
  void shouldNotBeRejectedByPredicateIfIncludedViaInclusions(String ext) {
    // Rejected extension due not apply at this level, they're only applied to secret checks in CheckContainer
    sensorContext.settings().setProperty(TEXT_INCLUSIONS_KEY, "**/*." + ext);
    InputFile inputFile = inputFile(Path.of("file." + ext), "");
    var actualMatchedFiles = textAndSecretsPredicateMatchingFiles(inputFile);
    assertThat(actualMatchedFiles).containsExactly(inputFile);
  }

  @ParameterizedTest
  @ValueSource(strings = {
    // binary files will not be excluded in default mode by the predicate
    ".keystore",
    "file.class"
  })
  void shouldAnalyzeTrackedHiddenFilesByDefault(String fileName) {
    // hidden tracked files will be analyzed regardless if a language is associated to it
    InputFile inputFile = hiddenInputFile(Path.of(".hidden", fileName), "");
    assertThat(textAndSecretsPredicateMatchingFiles(inputFile)).containsExactly(inputFile);
  }

  @Test
  void shouldMatchWhenInclusionsPropertyIsConfigured() {
    sensorContext.settings().setProperty(TEXT_INCLUSIONS_KEY, "*.txt,*.csv");
    var fooTxt = inputFile(Path.of("Foo.txt"), "");
    var fooCsv = inputFile(Path.of("Foo.csv"), "");
    var fooNope = inputFile(Path.of("Foo.nope"), "");
    assertThat(textAndSecretsPredicateMatchingFiles(fooTxt, fooCsv, fooNope)).containsExactlyInAnyOrder(fooTxt, fooCsv);
  }

  @Test
  void shouldNotMatchOnMultipleIncludedTextFileNamesWithoutAstrix() {
    sensorContext.settings().setProperty(TEXT_INCLUSIONS_KEY, ".txt,.csv");
    assertThat(textAndSecretsPredicateMatchingFiles(
      inputFile(Path.of("Foo.txt"), ""),
      inputFile(Path.of("Foo.csv"), ""),
      inputFile(Path.of("Foo.nope"), ""))).isEmpty();
  }

  @Test
  void shouldMatchOnDotEnvFile() {
    sensorContext.settings().setProperty(TEXT_INCLUSIONS_KEY, ".env");
    var dotEnv = inputFile(Path.of(".env"), "");
    var fooEnv = inputFile(Path.of("Foo.env"), "");
    var dotEnvironment = inputFile(Path.of(".environment"), "");
    var fooEnvironment = inputFile(Path.of("Foo.environment"), "");
    List<InputFile> actualMatchedFiles = textAndSecretsPredicateMatchingFiles(dotEnv, fooEnv, dotEnvironment, fooEnvironment);
    assertThat(actualMatchedFiles).containsExactly(dotEnv);
  }

  @Test
  void shouldMatchOnDotAwsConfig() {
    sensorContext.settings().setProperty(TEXT_INCLUSIONS_KEY, ".aws/config");
    var awsConfig = inputFile(Path.of(".aws", "config"), "");
    var awsDashConfig = inputFile(Path.of(".aws-config"), "");
    var awsConfiguration = inputFile(Path.of(".aws", "configuration"), "");
    var justConfig = inputFile(Path.of("config"), "");
    assertThat(textAndSecretsPredicateMatchingFiles(awsConfig, awsDashConfig, awsConfiguration, justConfig)).containsExactly(awsConfig);
  }

  @Test
  void shouldMatchOnDefaults() {
    sensorContext.settings().setProperty(TEXT_INCLUSIONS_KEY, TEXT_INCLUSIONS_DEFAULT_VALUE);
    var startSh = inputFile(Path.of("script", "start.sh"), "");
    var runBash = inputFile(Path.of("run.bash"), "");
    var aZsh = inputFile(Path.of("a.zsh"), "");
    var bKsh = inputFile(Path.of("b.ksh"), "");
    var winPs1 = inputFile(Path.of("win.ps1"), "");
    var myProperties = inputFile(Path.of("my.properties"), "");
    var someConf = inputFile(Path.of("config", "some.conf"), "");
    var myPem = inputFile(Path.of("my.pem"), "");
    var cccConfig = inputFile(Path.of("ccc.config"), "");
    var dotEnv = inputFile(Path.of(".env"), "");
    var somethingEnv = inputFile(Path.of("something.env"), "");
    var somethingDotEnv = inputFile(Path.of("something", ".env"), "");
    var awsConfig = inputFile(Path.of(".aws", "config"), "");

    List<InputFile> inputFiles = List.of(startSh, runBash, aZsh, bKsh, winPs1, myProperties, someConf, myPem, cccConfig, dotEnv, somethingEnv, somethingDotEnv, awsConfig);
    InputFile[] inputFilesArray = inputFiles.toArray(new InputFile[0]);

    List<InputFile> actualMatchedFiles = textAndSecretsPredicateMatchingFiles(inputFilesArray);
    assertThat(actualMatchedFiles).containsExactlyInAnyOrder(inputFilesArray);
  }

  @Test
  void shouldAnalyzeLanguageAssignedFilesInSonarQubeContext() {
    var file = inputFile(Path.of("Foo.java"), "", "java");
    assertThat(textAndSecretsPredicateMatchingFiles(file)).containsExactly(file);
  }

  @ParameterizedTest
  @ValueSource(strings = {
    // binary files are excluded in SQ-IDE context
    "file.class",
    ".keystore",
  })
  void shouldNotMatchTheFollowingFilesInSonarQubeIDEContext(String fileName) {
    sensorContext.setRuntime(SONARLINT_RUNTIME);
    InputFile inputFile = inputFile(Path.of(fileName), "");
    var actualMatchedFiles = textAndSecretsPredicateMatchingFiles(inputFile);
    assertThat(actualMatchedFiles).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(strings = {
    "fileWithoutExtension"
  })
  void shouldMatchTheFollowingFilesInSonarQubeIDEContext(String fileName) {
    sensorContext.setRuntime(SONARLINT_RUNTIME);
    InputFile inputFile = inputFile(Path.of(fileName), "");
    var actualMatchedFiles = textAndSecretsPredicateMatchingFiles(inputFile);
    assertThat(actualMatchedFiles).containsExactly(inputFile);
  }

  // ==========================================
  // excludedFileSuffixesPredicate
  // ==========================================

  @ParameterizedTest
  @CsvSource(value = {
    // rejected (in default excluded list)
    "file.md, false",
    "file.adoc, false",
    "file.html, false",
    "file.example, false",
    "file.sample, false",
    "file.template, false",
    "file.dist, false",
    "file.mdx, false",
    "file.MD, false",

    // accepted (not in default excluded list)
    "file.txt, true",
    "file.properties, true",
    "file.env, true",
    "file.conf, true",
    "file.yaml, true",
    "Dockerfile, true",
    "file, true",
    "file., true",
  })
  void excludedFileSuffixesPredicateShouldRejectFilesWithDefaultRejectedExtensionsCorrectly(String filename, boolean shouldAccept) {
    InputFile inputFile = TestUtils.inputFile(Path.of(filename), "");
    FilePredicate excludedFileSuffixesPredicate = textAndSecretsPredicates.excludedFileSuffixesPredicate(sensorContext);
    assertThat(excludedFileSuffixesPredicate.apply(inputFile)).isEqualTo(shouldAccept);
  }

  // ==========================================
  // Git predicate tests
  // ==========================================

  @Test
  void shouldNotCallGitFilePredicateInSonarlintContext() {
    sensorContext.setRuntime(SONARLINT_RUNTIME);
    var fileA = inputFile(Path.of("a.txt"), "");
    var fileB = inputFile(Path.of("b.txt"), "");
    assertThat(textAndSecretsPredicateMatchingFiles(fileA, fileB)).containsExactlyInAnyOrder(fileA, fileB);
    verify(gitService, times(0)).retrieveDirtyFileNames();
  }

  @Test
  void shouldCallGitFilePredicateOnDefault() {
    logTester.setLevel(Level.DEBUG);
    when(gitService.retrieveDirtyFileNames())
      .thenReturn(new GitService.DirtyFileNamesResult(true, Set.of("a.txt", "c.txt", "d.txt")));

    var fileA = inputFile(Path.of("a.txt"), "");
    var fileB = inputFile(Path.of("b.txt"), "");
    var fileC = inputFile(Path.of("c.txt"), "");
    var fileD = inputFile(Path.of("d.txt"), "");

    assertThat(textAndSecretsPredicateMatchingFiles(fileA, fileB, fileC, fileD)).containsExactly(fileB);
    textAndSecretsPredicates.logGitTrackedPredicateSummary();
    assertThat(logTester.logs()).contains(
      "Retrieving language associated files and files included via \"sonar.text.inclusions\" that are tracked by git",
      "3 files are ignored because they are untracked by git or has been modified",
      "Files untracked by git or modified:\n\ta.txt\n\tc.txt\n\td.txt");
  }

  @Test
  void shouldNotCallGitFilePredicateWhenInclusionsPropertyIsDeactivated() {
    sensorContext.settings().setProperty(INCLUSIONS_ACTIVATION_KEY, "false");
    var fileA = inputFile(Path.of("a.txt"), "", "secrets");
    var fileB = inputFile(Path.of("b.txt"), "", "secrets");
    var hiddenC = hiddenInputFile(Path.of("c.txt"), "", "secrets");
    var hiddenD = hiddenInputFile(Path.of("d.txt"), "");
    List<InputFile> actual = textAndSecretsPredicateMatchingFiles(fileA, fileB, hiddenC, hiddenD);
    assertThat(actual).containsExactlyInAnyOrder(fileA, fileB);
    assertThat(logTester.logs()).contains(
      "Retrieving only language associated files, \"sonar.text.inclusions.activate\" property is deactivated");
    verify(gitService, times(0)).retrieveDirtyFileNames();
  }

  @Test
  void shouldOnlyAnalyzeFilesBelongingToALanguageNoGitRepositoryIsFoundEvenIfInclusionsKeySet() {
    when(gitService.retrieveDirtyFileNames()).thenReturn(new GitService.DirtyFileNamesResult(false, Set.of()));

    var fileWithLanguage = inputFile(Path.of("a.txt"), "", "secrets");
    var fileWithoutLanguage = inputFile(Path.of("b.txt"), "");
    var hiddenWithLanguage = hiddenInputFile(Path.of("c.txt"), "", "secrets");
    var hiddenWithoutLanguage = hiddenInputFile(Path.of("d.txt"), "");

    assertThat(textAndSecretsPredicateMatchingFiles(fileWithLanguage, fileWithoutLanguage, hiddenWithLanguage, hiddenWithoutLanguage))
      .containsExactly(fileWithLanguage);
    assertThat(logTester.logs()).contains(
      "Retrieving only language associated files, " +
        "make sure to run the analysis inside a git repository to make use of inclusions specified via \"sonar.text.inclusions\"");
  }

  @Test
  void shouldNotAnalyzeUntrackedFilesNotBelongingToALanguage() {
    when(gitService.retrieveDirtyFileNames())
      .thenReturn(new GitService.DirtyFileNamesResult(true, Set.of("c.txt", "d.txt")));

    var trackedFileWithLanguage = inputFile(Path.of("a.txt"), "", "secrets");
    var trackedFileWithoutLanguage = inputFile(Path.of("b.txt"), "");
    var untrackedFileWithLanguage = inputFile(Path.of("c.txt"), "", "secrets");
    var untrackedFileWithoutLanguage = inputFile(Path.of("d.txt"), "");
    // no language and does not match any inclusions, so excluded regardless of git status
    var javaFile = inputFile(Path.of("src", "foo.java"), "");

    List<InputFile> actual = textAndSecretsPredicateMatchingFiles(trackedFileWithLanguage, trackedFileWithoutLanguage, untrackedFileWithLanguage,
      untrackedFileWithoutLanguage, javaFile);
    assertThat(actual).containsExactlyInAnyOrder(trackedFileWithLanguage, trackedFileWithoutLanguage, untrackedFileWithLanguage);
    textAndSecretsPredicates.logGitTrackedPredicateSummary();
    assertThat(logTester.logs()).contains("1 file is ignored because it is untracked by git or has been modified");
  }

  // ==========================================
  // Hidden file predicate behavior
  // ==========================================

  @Test
  void shouldAnalyzeAllTrackedHiddenFiles() {
    when(gitService.retrieveDirtyFileNames())
      .thenReturn(new GitService.DirtyFileNamesResult(true,
        Set.of(".untracked", ".hidden" + File.separator + "untracked.jks", ".untrackedSecrets")));

    var hiddenTrackedFile = hiddenInputFile(Path.of(".env"), "");
    var hiddenTrackedDirectoryFile = hiddenInputFile(Path.of(".hidden", "b.txt"), "");
    var hiddenUntrackedFile = hiddenInputFile(Path.of(".untracked"), "");
    var hiddenUntrackedFileWithLanguage = hiddenInputFile(Path.of(".untrackedSecrets"), "", "secrets");

    // binary file extensions are not discarded by the predicate, only in SQ-IDE context
    var hiddenBinaryFile = hiddenInputFile(Path.of(".keystore"), "");
    var hiddenUntrackedBinaryFile = hiddenInputFile(Path.of(".hidden", "untracked.jks"), "");

    List<InputFile> actual = textAndSecretsPredicateMatchingFiles(hiddenTrackedFile, hiddenTrackedDirectoryFile, hiddenUntrackedFile,
      hiddenUntrackedFileWithLanguage, hiddenBinaryFile, hiddenUntrackedBinaryFile);

    assertThat(actual).containsExactlyInAnyOrder(hiddenTrackedFile, hiddenTrackedDirectoryFile, hiddenBinaryFile);
  }

  @Test
  void shouldNotAnalyzeTrackedHiddenFilesWhenRuntimeDoesNotSupportIt() {
    sensorContext.setRuntime(SONARQUBE_RUNTIME_WITHOUT_HIDDEN_FILES_SUPPORT);

    var hiddenNonIncludedFile = hiddenInputFile(Path.of(".a"), "");
    var hiddenNonIncludedDirectoryFile = hiddenInputFile(Path.of(".hidden", "a.random"), "");
    var hiddenNonIncludedFileWithLanguage = hiddenInputFile(Path.of(".hidden", "b.random"), "", "secrets");
    var hiddenNonIncludedBinaryFile = hiddenInputFile(Path.of(".random"), "");
    var hiddenIncludedFile = hiddenInputFile(Path.of(".hidden", "a.txt"), "");
    var hiddenBinaryKeystore = hiddenInputFile(Path.of(".keystore"), "");

    List<InputFile> actual = textAndSecretsPredicateMatchingFiles(hiddenNonIncludedFile, hiddenNonIncludedDirectoryFile, hiddenNonIncludedFileWithLanguage,
      hiddenNonIncludedBinaryFile, hiddenIncludedFile, hiddenBinaryKeystore);

    // When runtime does not support hidden files, hidden files are analyzed as regular files.
    // In reality, these hidden files would not get picked up by the scanner at all, so they would not be analyzed.
    assertThat(actual).containsExactlyInAnyOrder(hiddenNonIncludedFileWithLanguage, hiddenIncludedFile);

    // When the runtime does not support hidden file analysis, isHidden() must never be consulted
    verify(hiddenNonIncludedFile, never()).isHidden();
    verify(hiddenNonIncludedDirectoryFile, never()).isHidden();
    verify(hiddenNonIncludedFileWithLanguage, never()).isHidden();
    verify(hiddenNonIncludedBinaryFile, never()).isHidden();
    verify(hiddenIncludedFile, never()).isHidden();
    verify(hiddenBinaryKeystore, never()).isHidden();
  }

  @Test
  void shouldNotAnalyzeGitInternalFiles() {
    var regularFile = inputFile(Path.of("a.txt"), "");
    var regularFileInGitLookingDirectory = inputFile(Path.of("workspace", ".github", "b.txt"), "");
    var gitInternalFile = hiddenInputFile(Path.of(".git", "config"), "");
    var gitNestedInternalFile = hiddenInputFile(Path.of("workspace", ".git", "config"), "");

    List<InputFile> actual = textAndSecretsPredicateMatchingFiles(regularFile, regularFileInGitLookingDirectory, gitInternalFile, gitNestedInternalFile);
    assertThat(actual).containsExactlyInAnyOrder(regularFile, regularFileInGitLookingDirectory);
  }

  // ==========================================
  // Predicate-owned telemetry
  // ==========================================

  @Test
  void shouldReportZeroTrackedTextFilesWhenGitStatusIsUnsuccessful() {
    when(gitService.retrieveDirtyFileNames()).thenReturn(new GitService.DirtyFileNamesResult(false, Set.of()));

    textAndSecretsPredicateMatchingFiles(
      inputFile(Path.of("a.txt"), "", "secrets"),
      inputFile(Path.of("b.txt"), ""),
      hiddenInputFile(Path.of("c.txt"), "", "secrets"),
      hiddenInputFile(Path.of("d.txt"), ""));

    textAndSecretsPredicates.reportAllTrackedTextFilesMeasure();
    verify(telemetryReporter).addNumericMeasure(TelemetryReporter.ALL_TRACKED_TEXT_FILES_MEASURE_KEY, 0);
  }

  // ==========================================
  // Helpers
  // ==========================================

  private List<InputFile> textAndSecretsPredicateMatchingFiles(InputFile... inputFiles) {
    for (InputFile f : inputFiles) {
      sensorContext.fileSystem().add(f);
    }
    var predicate = textAndSecretsPredicates.textAndSecretsPredicate();
    return StreamSupport.stream(sensorContext.fileSystem().inputFiles(predicate).spliterator(), false).toList();
  }

  private InputFile hiddenInputFile(Path path, @Nullable String content) {
    return hiddenInputFile(path, content, null);
  }

  private InputFile hiddenInputFile(Path path, @Nullable String content, @Nullable String language) {
    var inputFile = spy(inputFile(path, content, language));
    when(inputFile.isHidden()).thenReturn(true);
    return inputFile;
  }
}
