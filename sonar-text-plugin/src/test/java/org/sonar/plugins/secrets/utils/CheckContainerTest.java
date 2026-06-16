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
package org.sonar.plugins.secrets.utils;

import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.check.Rule;
import org.sonar.plugins.common.Check;
import org.sonar.plugins.common.InputFileContext;
import org.sonar.plugins.common.measures.DurationStatistics;
import org.sonar.plugins.secrets.api.AutomaticTestFileFilter;
import org.sonar.plugins.secrets.api.SecretsSpecificationLoader;
import org.sonar.plugins.secrets.api.SpecificationBasedCheck;
import org.sonar.plugins.secrets.api.filters.SkippedFilter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CheckContainerTest {

  private CheckContainer checkContainer;
  private SecretsSpecificationLoader specificationLoader;
  private DurationStatistics durationStatistics;
  private FilePredicate filePredicate;

  @BeforeEach
  void setUp() {
    checkContainer = new CheckContainer();
    specificationLoader = mock(SecretsSpecificationLoader.class);
    durationStatistics = mock(DurationStatistics.class);
    filePredicate = file -> true;
  }

  @Test
  void shouldInitializeSuccessfullyOnFirstCall() {
    var checks = List.of(new StubSpecificationBasedCheck(), new StubNonSpecificationCheck());
    when(durationStatistics.timed(anyString(), any(Supplier.class)))
      .thenAnswer(invocation -> ((Supplier<?>) invocation.getArgument(1)).get());

    checkContainer.initialize(checks, specificationLoader, durationStatistics, filePredicate);

    assertThat(checkContainer.isInitialized()).isTrue();
  }

  @Test
  void shouldSkipReInitializationOnSubsequentCalls() {
    var checks = List.of(new StubSpecificationBasedCheck(), new StubNonSpecificationCheck());
    when(durationStatistics.timed(anyString(), any(Supplier.class)))
      .thenAnswer(invocation -> ((Supplier<?>) invocation.getArgument(1)).get());

    // First initialization
    checkContainer.initialize(checks, specificationLoader, durationStatistics, filePredicate);
    assertThat(checkContainer.isInitialized()).isTrue();

    // Second initialization should be skipped
    checkContainer.initialize(checks, specificationLoader, durationStatistics, filePredicate);

    assertThat(checkContainer.isInitialized()).isTrue();
  }

  @Test
  void shouldThrowExceptionWhenAnalyzingWithoutInitialization() {
    var inputFileContext = mock(InputFileContext.class);
    var inputFile = mock(InputFile.class);
    when(inputFileContext.getInputFile()).thenReturn(inputFile);
    when(inputFile.filename()).thenReturn("filename");

    assertThatThrownBy(() -> checkContainer.analyze(inputFileContext))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("ChecksContainer must be initialized before use. Call initialize() first.");
  }

  @Test
  void shouldThrowExceptionWhenAnalyzingWithRuleIdWithoutInitialization() {
    var inputFileContext = mock(InputFileContext.class);
    var inputFile = mock(InputFile.class);
    when(inputFileContext.getInputFile()).thenReturn(inputFile);
    when(inputFile.filename()).thenReturn("filename");

    assertThatThrownBy(() -> checkContainer.analyze(inputFileContext, "ruleId"))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("ChecksContainer must be initialized before use. Call initialize() first.");
  }

  @Test
  void shouldReturnFalseForIsInitializedBeforeInitialization() {
    assertThat(checkContainer.isInitialized()).isFalse();
  }

  @Test
  void shouldRunOnlyNonSecretChecksWhenSecretsExclusionPredicateRejectsFile() {
    var secretCheck = spy(new StubSpecificationBasedCheck());
    doNothing().when(secretCheck).analyze(any(InputFileContext.class));
    var nonSecretCheck = spy(new StubNonSpecificationCheck());
    when(durationStatistics.timed(anyString(), any(Supplier.class)))
      .thenAnswer(invocation -> ((Supplier<?>) invocation.getArgument(1)).get());

    checkContainer.initialize(List.of(secretCheck, nonSecretCheck), specificationLoader, durationStatistics, file -> false);

    var inputFileContext = mock(InputFileContext.class);
    var inputFile = mock(InputFile.class);
    when(inputFileContext.getInputFile()).thenReturn(inputFile);

    checkContainer.analyze(inputFileContext);

    verify(secretCheck, never()).analyze(inputFileContext);
    verify(nonSecretCheck).analyze(inputFileContext);
    verify(inputFileContext).flushIssues();
  }

  @Test
  void shouldRunAllChecksWhenSecretsExclusionPredicateAcceptsFile() {
    var secretCheck = spy(new StubSpecificationBasedCheck());
    doNothing().when(secretCheck).analyze(any(InputFileContext.class));
    var nonSecretCheck = spy(new StubNonSpecificationCheck());
    when(durationStatistics.timed(anyString(), any(Supplier.class)))
      .thenAnswer(invocation -> ((Supplier<?>) invocation.getArgument(1)).get());

    checkContainer.initialize(List.of(secretCheck, nonSecretCheck), specificationLoader, durationStatistics, file -> true);

    var inputFileContext = mock(InputFileContext.class);
    var inputFile = mock(InputFile.class);
    when(inputFileContext.getInputFile()).thenReturn(inputFile);
    when(inputFileContext.content()).thenReturn("any content");

    checkContainer.analyze(inputFileContext);

    verify(secretCheck).analyze(inputFileContext);
    verify(nonSecretCheck).analyze(inputFileContext);
    verify(inputFileContext).flushIssues();
  }

  @Test
  void shouldSkipSecretChecksAndCountWhenAutoTestFileDetectionActiveAndFileIsTestFile() {
    var secretCheck = spy(new StubSpecificationBasedCheck());
    doNothing().when(secretCheck).analyze(any(InputFileContext.class));
    var nonSecretCheck = spy(new StubNonSpecificationCheck());
    when(durationStatistics.timed(anyString(), any(Supplier.class)))
      .thenAnswer(invocation -> ((Supplier<?>) invocation.getArgument(1)).get());

    checkContainer.initialize(List.of(secretCheck, nonSecretCheck), specificationLoader, durationStatistics, file -> true, true, Set.of(), true);

    var inputFileContext = mockInputFileContext("test_secret.txt", "/base/dir/");

    checkContainer.analyze(inputFileContext);

    verify(secretCheck, never()).analyze(inputFileContext);
    verify(nonSecretCheck).analyze(inputFileContext);
    verify(inputFileContext).flushIssues();
    assertThat(checkContainer.getAutomaticTestFilesSkippedCount()).isEqualTo(1);
    assertThat(checkContainer.getAutomaticTestFilesSkippedPaths()).hasSize(1);
    assertThat(checkContainer.isAutomaticTestFileFilterActive()).isTrue();
  }

  @Test
  void shouldRunSecretChecksAndNotCountWhenAutoTestFileDetectionActiveButFileIsNotTestFile() {
    var secretCheck = spy(new StubSpecificationBasedCheck());
    doNothing().when(secretCheck).analyze(any(InputFileContext.class));
    when(durationStatistics.timed(anyString(), any(Supplier.class)))
      .thenAnswer(invocation -> ((Supplier<?>) invocation.getArgument(1)).get());

    checkContainer.initialize(List.of(secretCheck), specificationLoader, durationStatistics, file -> true, true, Set.of(), false);

    var inputFileContext = mockInputFileContext("src/main/Foo.java", "/base/dir/");
    when(inputFileContext.content()).thenReturn("any content");

    checkContainer.analyze(inputFileContext);

    verify(secretCheck).analyze(inputFileContext);
    assertThat(checkContainer.getAutomaticTestFilesSkippedCount()).isZero();
    assertThat(checkContainer.getAutomaticTestFilesSkippedPaths()).isEmpty();
  }

  @Test
  void shouldRunSecretChecksAndNotCountWhenTestFilesFilterSkippedEvenForTestFile() {
    var secretCheck = spy(new StubSpecificationBasedCheck());
    doNothing().when(secretCheck).analyze(any(InputFileContext.class));
    when(durationStatistics.timed(anyString(), any(Supplier.class)))
      .thenAnswer(invocation -> ((Supplier<?>) invocation.getArgument(1)).get());

    checkContainer.initialize(List.of(secretCheck), specificationLoader, durationStatistics, file -> true, true, Set.of(SkippedFilter.TEST_FILES_FILTER), false);

    var inputFileContext = mockInputFileContext("test_secret.txt", "/base/dir/");
    when(inputFileContext.content()).thenReturn("any content");

    checkContainer.analyze(inputFileContext);

    verify(secretCheck).analyze(inputFileContext);
    assertThat(checkContainer.getAutomaticTestFilesSkippedCount()).isZero();
    assertThat(checkContainer.isAutomaticTestFileFilterActive()).isFalse();
  }

  @Test
  void shouldNotConsultTestFileClassificationWhenAutoTestFileDetectionDisabled() {
    var secretCheck = spy(new StubSpecificationBasedCheck());
    doNothing().when(secretCheck).analyze(any(InputFileContext.class));
    when(durationStatistics.timed(anyString(), any(Supplier.class)))
      .thenAnswer(invocation -> ((Supplier<?>) invocation.getArgument(1)).get());

    checkContainer.initialize(List.of(secretCheck), specificationLoader, durationStatistics, file -> true);

    var inputFileContext = mockInputFileContext("test_secret.txt", "/base/dir/");
    when(inputFileContext.content()).thenReturn("any content");

    checkContainer.analyze(inputFileContext);

    // With detection disabled the short-circuit means the classification is never even queried.
    verify(inputFileContext, never()).isAutomaticallyDetectedTestFile();
    assertThat(checkContainer.getAutomaticTestFilesSkippedCount()).isZero();
    assertThat(checkContainer.isAutomaticTestFileFilterActive()).isFalse();
  }

  @Test
  void shouldResetSkippedCountersOnSubsequentInitializeCalls() {
    var secretCheck = spy(new StubSpecificationBasedCheck());
    doNothing().when(secretCheck).analyze(any(InputFileContext.class));
    when(durationStatistics.timed(anyString(), any(Supplier.class)))
      .thenAnswer(invocation -> ((Supplier<?>) invocation.getArgument(1)).get());

    // First analysis: one skipped test file.
    checkContainer.initialize(List.of(secretCheck), specificationLoader, durationStatistics, file -> true, true, Set.of(), true);
    checkContainer.analyze(mockInputFileContext("test_one.txt", "/base/dir/"));
    assertThat(checkContainer.getAutomaticTestFilesSkippedCount()).isEqualTo(1);
    assertThat(checkContainer.getAutomaticTestFilesSkippedPaths()).hasSize(1);

    // A second initialize() starts a new analysis on this reused instance (e.g. a SonarLint module re-analysis — the
    // bean is @SonarLintSide(MODULE)). The trie-building portion short-circuits because the container is already
    // initialized, but the per-analysis skipped tracking must reset so the count is not carried over. The CLI's
    // multi-directory aggregation is handled separately in SecretFinder, so this reset does not affect it.
    checkContainer.initialize(List.of(secretCheck), specificationLoader, durationStatistics, file -> true, true, Set.of(), true);
    assertThat(checkContainer.getAutomaticTestFilesSkippedCount()).isZero();
    assertThat(checkContainer.getAutomaticTestFilesSkippedPaths()).isEmpty();
  }

  @Test
  void shouldNotPopulateSkippedPathsWhenCollectFlagOff() {
    var secretCheck = spy(new StubSpecificationBasedCheck());
    doNothing().when(secretCheck).analyze(any(InputFileContext.class));
    when(durationStatistics.timed(anyString(), any(Supplier.class)))
      .thenAnswer(invocation -> ((Supplier<?>) invocation.getArgument(1)).get());

    checkContainer.initialize(List.of(secretCheck), specificationLoader, durationStatistics, file -> true, true, Set.of(), false);

    checkContainer.analyze(mockInputFileContext("test_secret.txt", "/base/dir/"));

    // Count still increments — only the path list is gated by collectSkippedPaths.
    assertThat(checkContainer.getAutomaticTestFilesSkippedCount()).isEqualTo(1);
    assertThat(checkContainer.getAutomaticTestFilesSkippedPaths()).isEmpty();
  }

  private static InputFileContext mockInputFileContext(String relativePath, String baseDir) {
    var inputFile = mock(InputFile.class);
    when(inputFile.filename()).thenReturn(Path.of(relativePath).getFileName().toString());
    when(inputFile.uri()).thenReturn(URI.create("file:" + baseDir + relativePath));
    var fileSystem = mock(FileSystem.class);
    when(fileSystem.baseDir()).thenReturn(Path.of(baseDir).toFile());
    var sensorContext = mock(SensorContext.class);
    when(sensorContext.fileSystem()).thenReturn(fileSystem);
    var inputFileContext = mock(InputFileContext.class);
    when(inputFileContext.getInputFile()).thenReturn(inputFile);
    // CheckContainer derives the skipped-file relative path from the file URI + base dir, so expose the filesystem.
    when(inputFileContext.getFileSystem()).thenReturn(fileSystem);
    // CheckContainer reads the cached classification from the context; reproduce what the analyzer would compute and set.
    var isTestFile = AutomaticTestFileFilter.isAutomaticallyDetectedTestFile(sensorContext, inputFile);
    when(inputFileContext.isAutomaticallyDetectedTestFile()).thenReturn(isTestFile);
    return inputFileContext;
  }

  @Rule(key = "exampleSpecificationBasedCheck")
  private static class StubSpecificationBasedCheck extends SpecificationBasedCheck {
    @Override
    public String repositoryKey() {
      return "test-repo";
    }
  }

  @Rule(key = "exampleNonSpecificationBasedCheck")
  private static class StubNonSpecificationCheck extends Check {
    @Override
    public void analyze(InputFileContext inputFileContext) {
      // Stub for tests
    }

    @Override
    public String repositoryKey() {
      return "test-repo";
    }
  }
}
