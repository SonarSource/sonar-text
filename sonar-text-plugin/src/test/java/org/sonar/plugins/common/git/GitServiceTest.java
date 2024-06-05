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
package org.sonar.plugins.common.git;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.StatusCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.NoWorkTreeException;
import org.junit.jupiter.api.Test;
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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class GitServiceTest {
  @RegisterExtension
  LogTesterJUnit5 logTester = new LogTesterJUnit5();

  @Test
  void shouldRetrieveUntrackedFromRealJgit() throws IOException {
    var gitService = spy(new GitService());
    when(gitService.isGitCliAvailable()).thenReturn(false);

    var gitResult = gitService.retrieveUntrackedFileNames();
    assertThat(gitResult.isGitStatusSuccessful()).isTrue();
    assertThat(gitResult.untrackedFileNames()).isEmpty();
  }

  @ParameterizedTest
  @MethodSource
  void shouldRetrieveUntrackedFromJgit(Set<String> untrackedFiles) throws JgitSupplier.JgitInitializationException, IOException {
    var git = setupGitMock(untrackedFiles);
    var jgitSupplier = mock(JgitSupplier.class);
    when(jgitSupplier.getGit()).thenReturn(git);
    var gitService = spy(new GitService(jgitSupplier));
    when(gitService.isGitCliAvailable()).thenReturn(false);

    var gitResult = gitService.retrieveUntrackedFileNames();
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
    when(jgitSupplier.getGit()).thenThrow(expectedException);
    var gitService = spy(new GitService(jgitSupplier));
    when(gitService.isGitCliAvailable()).thenReturn(false);

    var gitResult = gitService.retrieveUntrackedFileNames();

    assertThat(gitResult.isGitStatusSuccessful()).isFalse();
    assertThat(logTester.logs(Level.DEBUG))
      .anyMatch(l -> l.contains("Using JGit to retrieve untracked files"))
      .anyMatch(l -> l.contains("Unable to retrieve git status"));

    logTester.setLevel(Level.INFO);
  }

  @Test
  void shouldRetrieveNoUntrackedFromCli() throws IOException {
    var gitService = spy(new GitService());
    if (gitService.isGitCliAvailable()) {
      var gitResult = gitService.retrieveUntrackedFileNames();
      assertThat(gitResult.isGitStatusSuccessful()).isTrue();
      assertThat(gitResult.untrackedFileNames())
        .as("No untracked files are expected in the test environment")
        .isEqualTo(Set.of());
    } else {
      throw new TestAbortedException("Git CLI is not available in the test environment");
    }
  }

  @Test
  void shouldRetrieveUntrackedFromCli() throws IOException {
    var gitService = spy(new GitService());
    if (gitService.isGitCliAvailable()) {
      when(gitService.getGitProcessBuilder(Arrays.asList("status", "--untracked-files=all", "--porcelain")))
        .thenReturn(new ProcessBuilderWrapper(List.of("echo", """
          M staged.txt
          ?? untracked.txt
          ?? untracked2""")));
      var gitResult = gitService.retrieveUntrackedFileNames();
      assertThat(gitResult.isGitStatusSuccessful()).isTrue();
      assertThat(gitResult.untrackedFileNames())
        .isEqualTo(Set.of("untracked.txt", "untracked2"));
    } else {
      throw new TestAbortedException("Git CLI is not available in the test environment");
    }
  }

  @Test
  void shouldNotUseWhenGitVersionFails() throws IOException {
    var gitService = spy(new GitService());
    var wrapper = mock(ProcessBuilderWrapper.class);
    when(wrapper.execute(any())).thenReturn(ProcessBuilderWrapper.Status.FAILURE);
    when(gitService.getGitProcessBuilder(List.of("--version"))).thenReturn(wrapper);

    assertThat(gitService.isGitCliAvailable()).isFalse();
    assertThat(logTester.logs(Level.DEBUG)).isEmpty();
  }

  @Test
  void shouldNotUseWhenGitVersionCrashes() throws IOException {
    var gitService = spy(new GitService());
    var wrapper = mock(ProcessBuilderWrapper.class);
    when(wrapper.execute(any())).thenThrow(new IOException("boom"));
    when(gitService.getGitProcessBuilder(List.of("--version"))).thenReturn(wrapper);

    assertThat(gitService.isGitCliAvailable()).isFalse();
    assertThat(logTester.logs(Level.DEBUG)).isEmpty();
  }

  @Test
  void shouldReturnFalseWhenGitStatusFails() throws IOException {
    var gitService = spy(new GitService());
    var wrapper = mock(ProcessBuilderWrapper.class);
    when(wrapper.execute(any())).thenReturn(ProcessBuilderWrapper.Status.SUCCESS, ProcessBuilderWrapper.Status.FAILURE);
    when(gitService.getGitProcessBuilder(anyList())).thenReturn(wrapper);

    var result = gitService.retrieveUntrackedFileNames();

    assertThat(result.isGitStatusSuccessful()).isFalse();
    assertThat(result.untrackedFileNames()).isEmpty();
    assertThat(logTester.logs(Level.DEBUG)).isEmpty();
  }

  @Test
  void shouldReturnFalseWhenGitStatusCrashes() throws IOException {
    var gitService = spy(new GitService());
    var wrapper = mock(ProcessBuilderWrapper.class);
    when(wrapper.execute(any())).thenReturn(ProcessBuilderWrapper.Status.SUCCESS)
      .thenThrow(new IOException("boom"));
    when(gitService.getGitProcessBuilder(anyList())).thenReturn(wrapper);

    var result = gitService.retrieveUntrackedFileNames();

    assertThat(result.isGitStatusSuccessful()).isFalse();
    assertThat(result.untrackedFileNames()).isEmpty();
    assertThat(logTester.logs(Level.DEBUG)).isEmpty();
  }

  @EnabledOnOs(OS.WINDOWS)
  @Test
  void shouldLocateGitOnWindows() throws IOException {
    var gitCommand = GitService.locateGitOnWindows();

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