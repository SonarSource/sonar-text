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
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.StatusCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.NoWorkTreeException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
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
import static org.sonar.plugins.common.TestUtils.inputFile;

public class GitServiceTest {
  @RegisterExtension
  LogTesterJUnit5 logTester = new LogTesterJUnit5();

  // Workaround to get the base directory of the project
  private static final Path BASE_DIR = inputFile(Paths.get("")).path();

  @Test
  void shouldRetrieveUntrackedFromRealJgit() throws IOException {
    var gitService = spy(new GitService());
    when(gitService.isGitCliAvailable()).thenReturn(false);

    var gitResult = gitService.retrieveUntrackedFileNames(BASE_DIR);
    assertThat(gitResult.isGitStatusSuccessful()).isTrue();
    assertThat(gitResult.untrackedFileNames()).isEmpty();
  }

  @ParameterizedTest
  @MethodSource
  void shouldRetrieveUntrackedFromJgit(Set<String> untrackedFiles) throws JgitSupplier.JgitInitializationException, IOException {
    var git = setupGitMock(untrackedFiles);
    var jgitSupplier = mock(JgitSupplier.class);
    when(jgitSupplier.getGit(any())).thenReturn(git);
    var gitService = spy(new GitService(jgitSupplier, new ProcessBuilderWrapper()));
    when(gitService.isGitCliAvailable()).thenReturn(false);

    var gitResult = gitService.retrieveUntrackedFileNames(BASE_DIR);
    assertThat(gitResult.isGitStatusSuccessful()).isTrue();
    assertThat(gitResult.untrackedFileNames()).isEqualTo(untrackedFiles);
  }

  static Stream<Arguments> shouldRetrieveUntrackedFromJgit() {
    String fooJavaPath = Path.of("src", "foo.java").toString();
    return Stream.of(
      Arguments.of(Set.of()),
      Arguments.of(Set.of("a.txt")),
      Arguments.of(Set.of("a.txt", "b.txt")),
      Arguments.of(Set.of(fooJavaPath)));
  }

  @ParameterizedTest
  @ValueSource(classes = {NoWorkTreeException.class, RuntimeException.class})
  void shouldReturnFalseWhenJgitException(Class<? extends Exception> exceptionClass) throws JgitSupplier.JgitInitializationException, IOException {
    logTester.setLevel(Level.DEBUG);

    var expectedException = new JgitSupplier.JgitInitializationException(mock(exceptionClass));
    var jgitSupplier = mock(JgitSupplier.class);
    when(jgitSupplier.getGit(any())).thenThrow(expectedException);
    var gitService = spy(new GitService(jgitSupplier, new ProcessBuilderWrapper()));
    when(gitService.isGitCliAvailable()).thenReturn(false);

    var gitResult = gitService.retrieveUntrackedFileNames(BASE_DIR);

    assertThat(gitResult.isGitStatusSuccessful()).isFalse();
    assertThat(logTester.logs())
      .anySatisfy(line -> assertThat(line).contains("Using JGit to retrieve untracked files"))
      .anySatisfy(line -> assertThat(line).contains("Exception querying Git data: " + expectedException.getMessage()));

    logTester.setLevel(Level.INFO);
  }

  @Test
  void shouldRetrieveNoUntrackedFromCli() throws IOException {
    var gitService = spy(new GitService());
    if (gitService.isGitCliAvailable()) {
      var gitResult = gitService.retrieveUntrackedFileNames(BASE_DIR);
      assertThat(gitResult.isGitStatusSuccessful()).isTrue();
      assertThat(gitResult.untrackedFileNames())
        .as("No untracked files are expected in the test environment")
        .isEqualTo(Set.of());
    } else {
      throw new TestAbortedException("Git CLI is not available in the test environment");
    }
  }

  @DisabledOnOs(OS.WINDOWS)
  @Test
  void shouldRetrieveUntrackedFromCli() throws IOException {
    var processBuilderWrapper = spy(new ProcessBuilderWrapper());
    var gitService = spy(new GitService(null, processBuilderWrapper));
    doAnswer(invocation -> processBuilderWrapper.execute(List.of("echo", """
      M staged.txt
      ?? untracked.txt
      ?? untracked2"""), invocation.getArgument(1)))
      .when(gitService).execute(eq(List.of("git", "status", "--untracked-files=all", "--porcelain")), any());
    var gitResult = gitService.retrieveUntrackedFileNames(BASE_DIR);
    assertThat(gitResult.isGitStatusSuccessful()).isTrue();
    assertThat(gitResult.untrackedFileNames())
      .containsOnly("untracked.txt", "untracked2");
  }

  @Test
  void shouldNotUseWhenGitVersionFails() throws IOException {
    var wrapper = mock(ProcessBuilderWrapper.class);
    var gitService = spy(new GitService(null, wrapper));
    when(wrapper.execute(any(), any())).thenReturn(ProcessBuilderWrapper.Status.FAILURE);

    assertThat(gitService.isGitCliAvailable()).isFalse();
    assertThat(logTester.logs(Level.DEBUG)).isEmpty();
  }

  @Test
  void shouldNotUseWhenGitVersionCrashes() throws IOException {
    var wrapper = mock(ProcessBuilderWrapper.class);
    var gitService = spy(new GitService(null, wrapper));
    when(gitService.execute(eq(List.of("--version")), any())).thenThrow(new IOException("boom"));

    assertThat(gitService.isGitCliAvailable()).isFalse();
    assertThat(logTester.logs(Level.DEBUG)).isEmpty();
  }

  @Test
  void shouldReturnFalseWhenGitStatusFails() throws IOException {
    var wrapper = mock(ProcessBuilderWrapper.class);
    var gitService = spy(new GitService(new JgitSupplier(), wrapper));

    if (GitService.isWindows()) {
      when(gitService.execute(any(), any())).thenReturn(
        ProcessBuilderWrapper.Status.SUCCESS, ProcessBuilderWrapper.Status.SUCCESS, ProcessBuilderWrapper.Status.FAILURE);
    } else {
      when(gitService.execute(any(), any())).thenReturn(ProcessBuilderWrapper.Status.SUCCESS, ProcessBuilderWrapper.Status.FAILURE);
    }
    var result = gitService.retrieveUntrackedFileNames(BASE_DIR);

    assertThat(result.isGitStatusSuccessful()).isFalse();
    assertThat(result.untrackedFileNames()).isEmpty();
    assertThat(logTester.logs(Level.DEBUG)).isEmpty();
  }

  @Test
  void shouldReturnFalseWhenGitStatusCrashes() throws IOException {
    var wrapper = mock(ProcessBuilderWrapper.class);
    var gitService = spy(new GitService(new JgitSupplier(), wrapper));
    if (GitService.isWindows()) {
      when(gitService.execute(any(), any())).thenReturn(
        ProcessBuilderWrapper.Status.SUCCESS, ProcessBuilderWrapper.Status.SUCCESS).thenThrow(new IOException("boom"));
    } else {
      when(gitService.execute(any(), any())).thenReturn(ProcessBuilderWrapper.Status.SUCCESS).thenThrow(new IOException("boom"));
    }

    var result = gitService.retrieveUntrackedFileNames(BASE_DIR);

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

    GitService gitService = new GitService(null, mockedWrapper);
    var gitCommand = gitService.locateGitOnWindows();

    assertThat(gitCommand).endsWith("\\git.exe");
  }

  @EnabledOnOs(OS.WINDOWS)
  @Test
  void shouldLocateGitOnWindows() throws IOException {
    var gitCommand = new GitService().locateGitOnWindows();

    assertThat(gitCommand).endsWith("\\git.exe");
  }

  public static Git setupGitMock(Set<String> untrackedFiles) {
    var git = mock(Git.class);
    var statusCommandMock = mock(StatusCommand.class);
    when(git.status()).thenReturn(statusCommandMock);
    var statusMock = mock(Status.class);
    try {
      when(statusCommandMock.call()).thenReturn(statusMock);
    } catch (GitAPIException e) {
      throw new RuntimeException(e);
    }
    when(statusMock.getUntracked()).thenReturn(untrackedFiles);
    return git;
  }

}
