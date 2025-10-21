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
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentest4j.TestAbortedException;
import org.slf4j.event.Level;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonar.plugins.common.git.utils.ProcessBuilderWrapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

class GitCliServiceTest {

  @RegisterExtension
  LogTesterJUnit5 logTester = new LogTesterJUnit5().setLevel(Level.DEBUG);

  private static final Path BASE_DIR_PLACEHOLDER = Paths.get("fake-repo-dir");

  @Test
  void shouldNotBeAvailableWhenGitVersionFails() throws IOException {
    var wrapper = mock(ProcessBuilderWrapper.class);
    when(wrapper.execute(any(), any())).thenReturn(ProcessBuilderWrapper.Status.FAILURE);

    try (var gitService = GitCliService.createOsSpecificInstance(BASE_DIR_PLACEHOLDER, wrapper)) {
      assertThat(gitService.isAvailable()).isFalse();
    }
    assertThatDebugLogsAreEmptyOrWindowsGitExeNotFound();
  }

  @Test
  void shouldNotBeAvailableWhenGitVersionCrashes() throws IOException {
    var wrapper = mock(ProcessBuilderWrapper.class);
    var gitService = spy(GitCliService.createOsSpecificInstance(BASE_DIR_PLACEHOLDER, wrapper));
    when(gitService.execute(eq(List.of("--version")), any())).thenThrow(new IOException("boom"));

    assertThat(gitService.isAvailable()).isFalse();
    assertThatDebugLogsAreEmptyOrWindowsGitExeNotFound();
  }

  @Test
  void shouldRetrieveNoUntrackedFilesFromCleanRepo(@TempDir Path tempDir) {
    GitRepoBuilder.setupCleanRepo(tempDir);

    try (var gitService = GitCliService.createOsSpecificInstance(tempDir)) {
      if (!gitService.isAvailable()) {
        throw new TestAbortedException("Git CLI is not available in the test environment");
      }
      var gitResult = gitService.retrieveUntrackedFileNames();
      assertThat(gitResult.isGitSuccessful()).isTrue();
      assertThat(gitResult.untrackedFileNames()).isEmpty();
    }
  }

  @Test
  void shouldRetrieveUntrackedFiles(@TempDir Path tempDir) {
    GitRepoBuilder.builder(tempDir)
      .withTrackedFile("tracked.txt")
      .withUntrackedFile("untracked.txt", "untracked2")
      .build();

    try (var gitService = GitCliService.createOsSpecificInstance(tempDir)) {
      if (!gitService.isAvailable()) {
        throw new TestAbortedException("Git CLI is not available in the test environment");
      }
      var gitResult = gitService.retrieveUntrackedFileNames();
      assertThat(gitResult.isGitSuccessful()).isTrue();
      assertThat(gitResult.untrackedFileNames())
        .containsOnly("untracked.txt", "untracked2");
    }
  }

  @Test
  void shouldReturnUnsuccessfulStatusWhenRetrievingUntrackedAndCliNotAvailable() throws IOException {
    var wrapper = mock(ProcessBuilderWrapper.class);
    when(wrapper.execute(any(), any())).thenReturn(ProcessBuilderWrapper.Status.FAILURE);
    var gitService = GitCliService.createOsSpecificInstance(BASE_DIR_PLACEHOLDER, wrapper);

    var result = gitService.retrieveUntrackedFileNames();
    gitService.close();

    assertThat(result).isEqualTo(GitService.UntrackedFileNamesResult.UNSUCCESSFUL);
    assertThatDebugLogsAreEmptyOrWindowsGitExeNotFound();
  }

  @Test
  void shouldReturnUnsuccessfulStatusWhenRetrievingUntrackedAndGitStatusFails() throws IOException {
    var wrapper = mock(ProcessBuilderWrapper.class);
    when(wrapper.execute(any(), any())).thenReturn(ProcessBuilderWrapper.Status.SUCCESS);
    var gitService = spy(GitCliService.createOsSpecificInstance(BASE_DIR_PLACEHOLDER, wrapper));

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
    var gitService = spy(GitCliService.createOsSpecificInstance(BASE_DIR_PLACEHOLDER, wrapper));

    when(gitService.execute(any(), any())).thenThrow(new IOException("boom"));
    var result = gitService.retrieveUntrackedFileNames();
    gitService.close();

    assertThat(result).isEqualTo(GitService.UntrackedFileNamesResult.UNSUCCESSFUL);
    assertThatDebugLogsAreEmptyOrWindowsGitExeNotFound();
  }

  @Test
  void shouldRetrieveRepositoryMetadataFromRealRepo(@TempDir Path tempDir) {
    GitRepoBuilder.setupRepoWithRemote(tempDir, "TestOrg", "test-project");

    try (var gitService = GitCliService.createOsSpecificInstance(tempDir)) {
      var gitResult = gitService.retrieveRepositoryMetadata();
      assertThat(gitResult.isGitSuccessful()).isTrue();
      assertThat(gitResult.projectName()).isEqualTo("test-project");
      assertThat(gitResult.organizationName()).isEqualTo("TestOrg");
    }
  }

  @ParameterizedTest
  @MethodSource
  void shouldReturnRepositoryMetadataFromValidRemotes(String remoteUrl, @TempDir Path tempDir) {
    GitRepoBuilder.builder(tempDir)
      .withTrackedFile("initial.txt")
      .withRemote("origin", remoteUrl)
      .build();

    try (var gitService = GitCliService.createOsSpecificInstance(tempDir)) {
      var result = gitService.retrieveRepositoryMetadata();
      assertThat(result.isGitSuccessful()).isTrue();
      assertThat(result.projectName()).isEqualTo("project");
      assertThat(result.organizationName()).isEqualTo("org");
    }
  }

  static Stream<String> shouldReturnRepositoryMetadataFromValidRemotes() {
    return Stream.of(
      "https://github.com/org/project.git",
      "https://github.com/org/project", // ".git" extension is not mandatory
      "git@github.com:org/project.git",
      "git://github.com/org/project.git",
      "/absolute/path/to/local/org/project",
      "../relative/path/to/local/org/project",
      "https://gitlab.com/org/project.git",
      "https://bitbucket.com/org/project.git");
  }

  @ParameterizedTest
  @MethodSource
  void shouldParseRepositoryMetadataFromAllRemoteLineFormats(String remoteLine) {
    var gitService = spy(GitCliService.createOsSpecificInstance(BASE_DIR_PLACEHOLDER));
    when(gitService.getGitRemotes()).thenReturn(Collections.singletonList(remoteLine));

    var result = gitService.retrieveRepositoryMetadata();
    gitService.close();

    assertThat(result.isGitSuccessful()).isTrue();
    assertThat(result.projectName()).isEqualTo("project");
    assertThat(result.organizationName()).isEqualTo("org");
  }

  static Stream<String> shouldParseRepositoryMetadataFromAllRemoteLineFormats() {
    return Stream.of(
      "origin https://github.com/org/project.git",
      "origin    https://github.com/org/project.git",
      "origin\thttps://github.com/org/project.git",
      "origin https://github.com/org/project.git (fetch)",
      "origin https://github.com/org/project.git (push)",
      "origin https://github.com/org/project.git (pull)",
      "origin https://github.com/org/project", // ".git" extension is not mandatory
      "origin git@github.com:org/project.git (fetch)",
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
    var gitService = GitCliService.createOsSpecificInstance(BASE_DIR_PLACEHOLDER, wrapper);

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
    var gitService = spy(GitCliService.createOsSpecificInstance(BASE_DIR_PLACEHOLDER, wrapper));

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
    var gitService = spy(GitCliService.createOsSpecificInstance(BASE_DIR_PLACEHOLDER, wrapper));

    when(gitService.execute(any(), any())).thenThrow(new IOException("boom"));
    var result = gitService.retrieveRepositoryMetadata();
    gitService.close();

    assertThat(result).isEqualTo(GitService.RepositoryMetadataResult.UNSUCCESSFUL);
    assertThatDebugLogsAreEmptyOrWindowsGitExeNotFound();
  }

  @ParameterizedTest
  @MethodSource
  void shouldReturnUnsuccessfulStatusWhenRetrievingRepositoryMetadataAndRemoteIsMalformed(String malformedRemote) {
    var gitService = spy(GitCliService.createOsSpecificInstance(BASE_DIR_PLACEHOLDER));

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

    try (var gitService = GitCliService.createOsSpecificInstance(BASE_DIR_PLACEHOLDER, mockedWrapper)) {
      assertThat(gitService.locateGitOnWindows()).endsWith("\\git.exe");
    }
  }

  @EnabledOnOs(OS.WINDOWS)
  @Test
  void shouldLocateGitOnWindows() {
    try (var gitService = GitCliService.createOsSpecificInstance(BASE_DIR_PLACEHOLDER)) {
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
