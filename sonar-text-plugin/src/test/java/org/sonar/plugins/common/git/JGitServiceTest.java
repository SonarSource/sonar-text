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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.stream.Stream;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.StatusCommand;
import org.eclipse.jgit.api.errors.CanceledException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.NoWorkTreeException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.event.Level;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.sonar.plugins.common.TestUtils.inputFileFromPath;

class JGitServiceTest {
  @RegisterExtension
  LogTesterJUnit5 logTester = new LogTesterJUnit5();

  // Workaround to get the base directory of the project
  private static final Path BASE_DIR = inputFileFromPath(Paths.get("")).path();

  @Test
  void shouldRetrieveUntrackedFromRealJGit() {
    try (var gitService = new JGitService(BASE_DIR)) {
      var gitResult = gitService.retrieveUntrackedFileNames();
      assertThat(gitResult.isGitStatusSuccessful()).isTrue();
      assertThat(gitResult.untrackedFileNames()).isEmpty();
    }
  }

  @ParameterizedTest
  @MethodSource
  void shouldRetrieveUntrackedFromJGit(Set<String> untrackedFiles) throws JGitSupplier.JGitInitializationException {
    var git = setupGitMock(untrackedFiles);
    var jgitSupplier = mock(JGitSupplier.class);
    when(jgitSupplier.getGit(any())).thenReturn(git);
    var gitService = spy(new JGitService(BASE_DIR, jgitSupplier));

    var gitResult = gitService.retrieveUntrackedFileNames();
    gitService.close();
    assertThat(gitResult.isGitStatusSuccessful()).isTrue();
    assertThat(gitResult.untrackedFileNames()).isEqualTo(untrackedFiles);
  }

  static Stream<Arguments> shouldRetrieveUntrackedFromJGit() {
    String fooJavaPath = Path.of("src", "foo.java").toString();
    return Stream.of(
      Arguments.of(Set.of()),
      Arguments.of(Set.of("a.txt")),
      Arguments.of(Set.of("a.txt", "b.txt")),
      Arguments.of(Set.of(fooJavaPath)));
  }

  @ParameterizedTest
  @ValueSource(classes = {NoWorkTreeException.class, RuntimeException.class})
  void shouldReturnUnsuccessfulStatusWhenJGitException(Class<? extends Exception> exceptionClass) throws JGitSupplier.JGitInitializationException {
    logTester.setLevel(Level.DEBUG);

    var expectedException = new JGitSupplier.JGitInitializationException(mock(exceptionClass));
    var jgitSupplier = mock(JGitSupplier.class);
    when(jgitSupplier.getGit(any())).thenThrow(expectedException);
    var gitService = spy(new JGitService(BASE_DIR, jgitSupplier));

    var gitResult = gitService.retrieveUntrackedFileNames();
    gitService.close();

    assertThat(gitResult.isGitStatusSuccessful()).isFalse();
    assertThat(logTester.logs())
      .anySatisfy(line -> assertThat(line).contains("Exception querying Git data: " + expectedException.getMessage()));

    logTester.setLevel(Level.INFO);
  }

  @Test
  void shouldReturnUnsuccessfulStatusWhenGitApiException() throws GitAPIException {
    logTester.setLevel(Level.DEBUG);

    var git = setupGitMock(Set.of("a.txt"));
    var expectedException = new CanceledException("canceled");
    when(git.status().call()).thenThrow(expectedException);

    var jgitSupplier = mock(JGitSupplier.class);
    when(jgitSupplier.getGit(any())).thenReturn(git);

    var gitService = spy(new JGitService(BASE_DIR, jgitSupplier));
    var gitResult = gitService.retrieveUntrackedFileNames();
    gitService.close();

    assertThat(gitResult.isGitStatusSuccessful()).isFalse();
    assertThat(logTester.logs())
      .anySatisfy(line -> assertThat(line).contains("Exception querying Git data: " + expectedException.getMessage()));

    logTester.setLevel(Level.INFO);
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
