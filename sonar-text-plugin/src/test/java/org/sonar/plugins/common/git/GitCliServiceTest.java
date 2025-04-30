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
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;
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

class GitCliServiceTest {
  @RegisterExtension
  LogTesterJUnit5 logTester = new LogTesterJUnit5();

  @Test
  void shouldRetrieveNoUntrackedFiles() {
    try (var gitService = GitCliService.createOsSpecificInstance()) {
      if (gitService.isAvailable()) {
        var gitResult = gitService.retrieveUntrackedFileNames();
        assertThat(gitResult.isGitStatusSuccessful()).isTrue();
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
    var gitService = spy(GitCliService.createOsSpecificInstance(processBuilderWrapper));
    doAnswer(invocation -> processBuilderWrapper.execute(List.of("echo", """
      M staged.txt
      ?? untracked.txt
      ?? untracked2"""), invocation.getArgument(1)))
      .when(gitService).execute(eq(List.of("git", "status", "--untracked-files=all", "--porcelain")), any());
    var gitResult = gitService.retrieveUntrackedFileNames();
    gitService.close();
    assertThat(gitResult.isGitStatusSuccessful()).isTrue();
    assertThat(gitResult.untrackedFileNames())
      .containsOnly("untracked.txt", "untracked2");
  }

  @Test
  void shouldNotBeAvailableWhenGitVersionFails() throws IOException {
    var wrapper = mock(ProcessBuilderWrapper.class);
    when(wrapper.execute(any(), any())).thenReturn(ProcessBuilderWrapper.Status.FAILURE);

    try (var gitService = GitCliService.createOsSpecificInstance(wrapper)) {
      assertThat(gitService.isAvailable()).isFalse();
    }
    assertThat(logTester.logs(Level.DEBUG)).isEmpty();
  }

  @Test
  void shouldNotBeAvailableWhenGitVersionCrashes() throws IOException {
    var wrapper = mock(ProcessBuilderWrapper.class);
    var gitService = spy(GitCliService.createOsSpecificInstance(wrapper));
    when(gitService.execute(eq(List.of("--version")), any())).thenThrow(new IOException("boom"));

    assertThat(gitService.isAvailable()).isFalse();
    assertThat(logTester.logs(Level.DEBUG)).isEmpty();
  }

  @Test
  void shouldReturnUnsuccessfulStatusWhenNotAvailable() throws IOException {
    var wrapper = mock(ProcessBuilderWrapper.class);
    when(wrapper.execute(any(), any())).thenReturn(ProcessBuilderWrapper.Status.FAILURE);
    var gitService = spy(GitCliService.createOsSpecificInstance(wrapper));

    var result = gitService.retrieveUntrackedFileNames();
    gitService.close();

    assertThat(result.isGitStatusSuccessful()).isFalse();
    assertThat(result.untrackedFileNames()).isEmpty();
    assertThat(logTester.logs(Level.DEBUG)).isEmpty();
  }

  @Test
  void shouldReturnUnsuccessfulStatusWhenGitStatusFails() throws IOException {
    var wrapper = mock(ProcessBuilderWrapper.class);
    when(wrapper.execute(any(), any())).thenReturn(ProcessBuilderWrapper.Status.SUCCESS);
    var gitService = spy(GitCliService.createOsSpecificInstance(wrapper));

    when(gitService.execute(any(), any())).thenReturn(ProcessBuilderWrapper.Status.FAILURE);
    var result = gitService.retrieveUntrackedFileNames();
    gitService.close();

    assertThat(result.isGitStatusSuccessful()).isFalse();
    assertThat(result.untrackedFileNames()).isEmpty();
    assertThat(logTester.logs(Level.DEBUG)).isEmpty();
  }

  @Test
  void shouldReturnUnsuccessfulStatusWhenGitStatusCrashes() throws IOException {
    var wrapper = mock(ProcessBuilderWrapper.class);
    when(wrapper.execute(any(), any())).thenReturn(ProcessBuilderWrapper.Status.SUCCESS);
    var gitService = spy(GitCliService.createOsSpecificInstance(wrapper));

    when(gitService.execute(any(), any())).thenThrow(new IOException("boom"));
    var result = gitService.retrieveUntrackedFileNames();
    gitService.close();

    assertThat(result.isGitStatusSuccessful()).isFalse();
    assertThat(result.untrackedFileNames()).isEmpty();
    assertThat(logTester.logs(Level.DEBUG)).isEmpty();
  }

  @Test
  void shouldLocateGitOnWindowsWithMockedCall() throws IOException {
    ProcessBuilderWrapper mockedWrapper = mock(ProcessBuilderWrapper.class);
    when(mockedWrapper.execute(any(), any())).thenAnswer(invocationOnMock -> {
      Consumer<String> consumer = invocationOnMock.getArgument(1);
      consumer.accept("C:\\path\\to\\git.exe");
      return ProcessBuilderWrapper.Status.SUCCESS;
    });

    try (var gitService = GitCliService.createOsSpecificInstance(mockedWrapper)) {
      assertThat(gitService.locateGitOnWindows()).endsWith("\\git.exe");
    }
  }

  @EnabledOnOs(OS.WINDOWS)
  @Test
  void shouldLocateGitOnWindows() {
    try (var gitService = GitCliService.createOsSpecificInstance()) {
      assertThat(gitService.getGitCommand()).endsWith("\\git.exe");
    }
  }
}
