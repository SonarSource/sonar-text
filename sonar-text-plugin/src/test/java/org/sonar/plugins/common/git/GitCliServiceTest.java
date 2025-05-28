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
package org.sonar.plugins.common.git;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentest4j.TestAbortedException;
import org.slf4j.event.Level;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonar.plugins.common.git.utils.ProcessBuilderWrapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.sonar.plugins.common.TestUtils.inputFileFromPath;

class GitCliServiceTest {

  @RegisterExtension
  LogTesterJUnit5 logTester = new LogTesterJUnit5().setLevel(Level.DEBUG);

  // Workaround to get the base directory of the project
  private static final Path BASE_DIR = inputFileFromPath(Paths.get("")).path();

  @Test
  void shouldNotBeAvailableWhenGitVersionFails() throws IOException {
    var wrapper = mock(ProcessBuilderWrapper.class);
    when(wrapper.execute(any(), any())).thenReturn(ProcessBuilderWrapper.Status.FAILURE);

    try (var gitService = GitCliService.createOsSpecificInstance(BASE_DIR, wrapper)) {
      assertThat(gitService.isAvailable()).isFalse();
    }
    assertThatDebugLogsAreEmptyOrWindowsGitExeNotFound();
  }

  @Test
  void shouldNotBeAvailableWhenGitVersionCrashes() throws IOException {
    var wrapper = mock(ProcessBuilderWrapper.class);
    var gitService = spy(GitCliService.createOsSpecificInstance(BASE_DIR, wrapper));
    when(gitService.execute(eq(List.of("--version")), any())).thenThrow(new IOException("boom"));

    assertThat(gitService.isAvailable()).isFalse();
    assertThatDebugLogsAreEmptyOrWindowsGitExeNotFound();
  }

  @Test
  void shouldRetrieveNoUntrackedFiles() {
    try (var gitService = GitCliService.createOsSpecificInstance(BASE_DIR)) {
      if (gitService.isAvailable()) {
        var gitResult = gitService.retrieveUntrackedFileNames();
        assertThat(gitResult.isGitSuccessful()).isTrue();
        assertThat(gitResult.untrackedFileNames())
          .as("No untracked files are expected in the test environment")
          .isEqualTo(Set.of());
      } else {
        throw new TestAbortedException("Git CLI is not available in the test environment");
      }
    }
  }

  @DisabledOnOs(OS.WINDOWS)
  @Test
  void shouldRetrieveUntrackedFiles() throws IOException {
    var processBuilderWrapper = spy(new ProcessBuilderWrapper());
    var gitService = spy(GitCliService.createOsSpecificInstance(BASE_DIR, processBuilderWrapper));
    doAnswer(invocation -> processBuilderWrapper.execute(List.of("echo", """
      M staged.txt
      ?? untracked.txt
      ?? untracked2"""), invocation.getArgument(1)))
      .when(gitService).execute(eq(List.of("git", "-C", BASE_DIR.toAbsolutePath().toString(), "status", "--untracked-files=all", "--porcelain")), any());
    var gitResult = gitService.retrieveUntrackedFileNames();
    gitService.close();
    assertThat(gitResult.isGitSuccessful()).isTrue();
    assertThat(gitResult.untrackedFileNames())
      .containsOnly("untracked.txt", "untracked2");
  }

  @Test
  void shouldReturnUnsuccessfulStatusWhenRetrievingUntrackedAndCliNotAvailable() throws IOException {
    var wrapper = mock(ProcessBuilderWrapper.class);
    when(wrapper.execute(any(), any())).thenReturn(ProcessBuilderWrapper.Status.FAILURE);
    var gitService = GitCliService.createOsSpecificInstance(BASE_DIR, wrapper);

    var result = gitService.retrieveUntrackedFileNames();
    gitService.close();

    assertThat(result).isEqualTo(GitService.UntrackedFileNamesResult.UNSUCCESSFUL);
    assertThatDebugLogsAreEmptyOrWindowsGitExeNotFound();
  }

  @Test
  void shouldReturnUnsuccessfulStatusWhenRetrievingUntrackedAndGitStatusFails() throws IOException {
    var wrapper = mock(ProcessBuilderWrapper.class);
    when(wrapper.execute(any(), any())).thenReturn(ProcessBuilderWrapper.Status.SUCCESS);
    var gitService = spy(GitCliService.createOsSpecificInstance(BASE_DIR, wrapper));

    when(gitService.execute(any(), any())).thenReturn(ProcessBuilderWrapper.Status.FAILURE);
    var result = gitService.retrieveUntrackedFileNames();
    gitService.close();

    assertThat(result).isEqualTo(GitService.UntrackedFileNamesResult.UNSUCCESSFUL);
    assertThatDebugLogsAreEmptyOrWindowsGitExeNotFound();
  }

  @Test
  void shouldReturnUnsuccessfulStatusWhenRetrievingUntrackedAndGitStatusCrashes() throws IOException {
    var wrapper = mock(ProcessBuilderWrapper.class);
    when(wrapper.execute(any(), any())).thenReturn(ProcessBuilderWrapper.Status.SUCCESS);
    var gitService = spy(GitCliService.createOsSpecificInstance(BASE_DIR, wrapper));

    when(gitService.execute(any(), any())).thenThrow(new IOException("boom"));
    var result = gitService.retrieveUntrackedFileNames();
    gitService.close();

    assertThat(result).isEqualTo(GitService.UntrackedFileNamesResult.UNSUCCESSFUL);
    assertThatDebugLogsAreEmptyOrWindowsGitExeNotFound();
  }

  @Test
  void shouldRetrieveRepositoryMetadata() {
    try (var gitService = GitCliService.createOsSpecificInstance(BASE_DIR)) {
      if (gitService.isAvailable()) {
        var gitResult = gitService.retrieveRepositoryMetadata();
        assertThat(gitResult.isGitSuccessful()).isTrue();
        assertThat(gitResult.projectName()).startsWith("sonar-text");
        assertThat(gitResult.organizationName()).isEqualTo("SonarSource");
      } else {
        throw new TestAbortedException("Git CLI is not available in the test environment");
      }
    }
  }

  @ParameterizedTest
  @MethodSource
  void shouldReturnRepositoryMetadataFromValidRemotes(String remote) {
    var gitService = spy(GitCliService.createOsSpecificInstance(BASE_DIR));

    when(gitService.getGitRemotes()).thenReturn(Collections.singletonList(remote));
    var result = gitService.retrieveRepositoryMetadata();
    gitService.close();

    assertThat(result.isGitSuccessful()).isTrue();
    assertThat(result.projectName()).isEqualTo("project");
    assertThat(result.organizationName()).isEqualTo("org");
  }

  static Stream<String> shouldReturnRepositoryMetadataFromValidRemotes() {
    return Stream.of(
      "origin https://github.com/org/project.git",
      "origin    https://github.com/org/project.git",
      "origin https://github.com/org/project.git (fetch)",
      "origin https://github.com/org/project.git (push)",
      "origin https://github.com/org/project.git (pull)",
      "origin https://github.com/org/project", // ".git" extension is not mandatory
      "origin  git@github.com:org/project.git (fetch)",
      "origin  git://github.com/org/project.git (fetch)",
      "origin  /absolute/path/to/local/org/project (fetch)",
      "origin  ../relative/path/to/local/org/project (fetch)",
      "other https://github.com/org/project.git",
      "origin https://gitlab.com/org/project.git",
      "origin https://bitbucket.com/org/project.git");
  }

  @Test
  void shouldReturnUnsuccessfulStatusWhenRetrievingRepositoryMetadataAndCliNotAvailable() throws IOException {
    var wrapper = mock(ProcessBuilderWrapper.class);
    when(wrapper.execute(any(), any())).thenReturn(ProcessBuilderWrapper.Status.FAILURE);
    var gitService = GitCliService.createOsSpecificInstance(BASE_DIR, wrapper);

    var result = gitService.retrieveRepositoryMetadata();
    gitService.close();

    assertThat(result.isGitSuccessful()).isFalse();
    assertThat(result.projectName()).isEmpty();
    assertThat(result.organizationName()).isEmpty();
    assertThatDebugLogsAreEmptyOrWindowsGitExeNotFound();
  }

  @Test
  void shouldReturnUnsuccessfulStatusWhenRetrievingRepositoryMetadataAndGitCommandFails() throws IOException {
    var wrapper = mock(ProcessBuilderWrapper.class);
    when(wrapper.execute(any(), any())).thenReturn(ProcessBuilderWrapper.Status.SUCCESS);
    var gitService = spy(GitCliService.createOsSpecificInstance(BASE_DIR, wrapper));

    when(gitService.execute(any(), any())).thenReturn(ProcessBuilderWrapper.Status.FAILURE);
    var result = gitService.retrieveRepositoryMetadata();
    gitService.close();

    assertThat(result).isEqualTo(GitService.RepositoryMetadataResult.UNSUCCESSFUL);
    assertThatDebugLogsAreEmptyOrWindowsGitExeNotFound();
  }

  @Test
  void shouldReturnUnsuccessfulStatusWhenRetrievingRepositoryMetadataAndGitCommandCrashes() throws IOException {
    var wrapper = mock(ProcessBuilderWrapper.class);
    when(wrapper.execute(any(), any())).thenReturn(ProcessBuilderWrapper.Status.SUCCESS);
    var gitService = spy(GitCliService.createOsSpecificInstance(BASE_DIR, wrapper));

    when(gitService.execute(any(), any())).thenThrow(new IOException("boom"));
    var result = gitService.retrieveRepositoryMetadata();
    gitService.close();

    assertThat(result).isEqualTo(GitService.RepositoryMetadataResult.UNSUCCESSFUL);
    assertThatDebugLogsAreEmptyOrWindowsGitExeNotFound();
  }

  @ParameterizedTest
  @MethodSource
  void shouldReturnUnsuccessfulStatusWhenRetrievingRepositoryMetadataAndRemoteIsMalformed(String malformedRemote) {
    var gitService = spy(GitCliService.createOsSpecificInstance(BASE_DIR));

    when(gitService.getGitRemotes()).thenReturn(Collections.singletonList(malformedRemote));
    var result = gitService.retrieveRepositoryMetadata();
    gitService.close();

    assertThat(result).isEqualTo(GitService.RepositoryMetadataResult.UNSUCCESSFUL);
    assertThat(logTester.logs(Level.DEBUG))
      .anySatisfy(line -> assertThat(line).contains("Failed to parse repository metadata from remote"));
  }

  static Stream<String> shouldReturnUnsuccessfulStatusWhenRetrievingRepositoryMetadataAndRemoteIsMalformed() {
    return Stream.of(
      "", // Empty remote
      "origin extra https://github.com/org/project.git", // Extra fields in the remote
      "https://github.com/org/project.git" // Remote without name
    );
  }

  @Test
  void shouldLocateGitOnWindowsWithMockedCall() throws IOException {
    ProcessBuilderWrapper mockedWrapper = mock(ProcessBuilderWrapper.class);
    when(mockedWrapper.execute(any(), any())).thenAnswer(invocationOnMock -> {
      Consumer<String> consumer = invocationOnMock.getArgument(1);
      consumer.accept("C:\\path\\to\\git.exe");
      return ProcessBuilderWrapper.Status.SUCCESS;
    });

    try (var gitService = GitCliService.createOsSpecificInstance(BASE_DIR, mockedWrapper)) {
      assertThat(gitService.locateGitOnWindows()).endsWith("\\git.exe");
    }
  }

  @EnabledOnOs(OS.WINDOWS)
  @Test
  void shouldLocateGitOnWindows() {
    try (var gitService = GitCliService.createOsSpecificInstance(BASE_DIR)) {
      assertThat(gitService.getGitCommand()).endsWith("\\git.exe");
    }
  }

  private void assertThatDebugLogsAreEmptyOrWindowsGitExeNotFound() {
    var debugLogs = logTester.logs(Level.DEBUG);
    if (OS.WINDOWS.isCurrentOs()) {
      assertThat(debugLogs).hasSize(1);
      assertThat(debugLogs.get(0)).startsWith("git.exe not found in PATH. PATH value was: ");
    } else {
      assertThat(debugLogs).isEmpty();
    }
  }
}
