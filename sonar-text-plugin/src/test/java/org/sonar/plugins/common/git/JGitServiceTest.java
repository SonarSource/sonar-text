/*
 * SonarQube Text Plugin
 * Copyright (C) 2021-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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
import java.util.Collections;
import java.util.Set;
import java.util.stream.Stream;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.RemoteListCommand;
import org.eclipse.jgit.api.StatusCommand;
import org.eclipse.jgit.api.errors.CanceledException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.NoWorkTreeException;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
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

class JGitServiceTest {

  @RegisterExtension
  LogTesterJUnit5 logTester = new LogTesterJUnit5().setLevel(Level.DEBUG);

  private static final Path BASE_DIR_PLACEHOLDER = Paths.get("fake-repo-dir");

  @Test
  void shouldRetrieveNoDirtyFilesFromCleanRepo(@TempDir Path tempDir) {
    GitRepoBuilder.setupCleanRepo(tempDir);

    try (var gitService = new JGitService(tempDir)) {
      var gitResult = gitService.retrieveDirtyFileNames();
      assertThat(gitResult.isGitSuccessful()).isTrue();
      // We expect an empty file list, but in the CI there may be additional files related to gradle build
      assertThat(gitResult.dirtyFileNames())
        .allMatch(f -> f.equals("build_number.txt") || f.equals("gradle.properties.bak") || f.equals("null/null/evaluated_project_version.txt"));
    }
  }

  @ParameterizedTest
  @MethodSource
  void shouldRetrieveUntrackedFromJGit(Set<String> untrackedFiles, @TempDir Path tempDir) {
    var builder = GitRepoBuilder.builder(tempDir).withTrackedFile("tracked.txt");
    untrackedFiles.forEach(builder::withUntrackedFile);
    builder.build();

    try (var gitService = new JGitService(tempDir)) {
      var gitResult = gitService.retrieveDirtyFileNames();
      assertThat(gitResult.isGitSuccessful()).isTrue();
      assertThat(gitResult.dirtyFileNames()).isEqualTo(untrackedFiles);
    }
  }

  static Stream<Arguments> shouldRetrieveUntrackedFromJGit() {
    return Stream.of(
      Arguments.of(Set.of()),
      Arguments.of(Set.of("a.txt")),
      Arguments.of(Set.of("a.txt", "b.txt")),
      Arguments.of(Set.of("src/foo.java")));
  }

  @Test
  void shouldRetrieveModifiedFilesFromJGit(@TempDir Path tempDir) {
    GitRepoBuilder.builder(tempDir)
      .withTrackedFile("tracked.txt")
      .withUnstagedModifiedFile("unstagedModified.txt")
      .withStagedModifiedFile("stagedModified.txt")
      .withStagedThenModifiedFile("stagedThenModified.txt")
      .build();

    try (var gitService = new JGitService(tempDir)) {
      var gitResult = gitService.retrieveDirtyFileNames();
      assertThat(gitResult.isGitSuccessful()).isTrue();
      assertThat(gitResult.dirtyFileNames()).containsOnly("unstagedModified.txt", "stagedModified.txt", "stagedThenModified.txt");
    }
  }

  @ParameterizedTest
  @ValueSource(classes = {NoWorkTreeException.class, RuntimeException.class})
  void shouldReturnUnsuccessfulStatusWhenRetrievingDirtyAndJGitNotInit(Class<? extends Exception> exceptionClass) throws JGitSupplier.JGitInitializationException {
    var expectedException = new JGitSupplier.JGitInitializationException(mock(exceptionClass));
    var jgitSupplier = mock(JGitSupplier.class);
    when(jgitSupplier.getGit(any())).thenThrow(expectedException);
    var gitService = new JGitService(BASE_DIR_PLACEHOLDER, jgitSupplier);

    var gitResult = gitService.retrieveDirtyFileNames();
    gitService.close();

    assertThat(gitResult).isEqualTo(GitService.DirtyFileNamesResult.UNSUCCESSFUL);
    assertThat(logTester.logs())
      .anySatisfy(line -> assertThat(line).contains("Exception querying Git data: " + expectedException.getMessage()));
  }

  @Test
  void shouldReturnUnsuccessfulStatusWhenRetrievingDirtyFails() throws GitAPIException {
    var git = mock(Git.class);
    var status = mock(StatusCommand.class);
    when(git.status()).thenReturn(status);
    var expectedException = new CanceledException("canceled");
    when(status.call()).thenThrow(expectedException);

    var jgitSupplier = mock(JGitSupplier.class);
    when(jgitSupplier.getGit(any())).thenReturn(git);

    var gitService = new JGitService(BASE_DIR_PLACEHOLDER, jgitSupplier);
    var gitResult = gitService.retrieveDirtyFileNames();
    gitService.close();

    assertThat(gitResult).isEqualTo(GitService.DirtyFileNamesResult.UNSUCCESSFUL);
    assertThat(logTester.logs())
      .anySatisfy(line -> assertThat(line).contains("Exception querying Git data: " + expectedException.getMessage()));
  }

  @Test
  void shouldRetrieveRepositoryMetadataFromRealRepo(@TempDir Path tempDir) {
    GitRepoBuilder.setupRepoWithRemote(tempDir, "TestOrg", "test-project");

    try (var gitService = new JGitService(tempDir)) {
      var gitResult = gitService.retrieveRepositoryMetadata();
      assertThat(gitResult.isGitSuccessful()).isTrue();
      assertThat(gitResult.projectName()).isEqualTo("test-project");
      assertThat(gitResult.organizationName()).isEqualTo("TestOrg");
    }
  }

  @ParameterizedTest
  @MethodSource
  void shouldRetrieveRepositoryMetadataFromValidRemoteUri(String remoteUri, @TempDir Path tempDir) {
    GitRepoBuilder.builder(tempDir)
      .withTrackedFile("initial.txt")
      .withRemote("origin", remoteUri)
      .build();

    try (var gitService = new JGitService(tempDir)) {
      var result = gitService.retrieveRepositoryMetadata();
      assertThat(result.isGitSuccessful()).isTrue();
      assertThat(result.projectName()).isEqualTo("project");
      assertThat(result.organizationName()).isEqualTo("org");
    }
  }

  static Stream<String> shouldRetrieveRepositoryMetadataFromValidRemoteUri() {
    return Stream.of(
      "/org/project.git",
      "/org/project", // ".git" extension is not mandatory
      "/extra/org/project.git");
  }

  @ParameterizedTest
  @ValueSource(classes = {NoWorkTreeException.class, RuntimeException.class})
  void shouldReturnUnsuccessfulStatusWhenRetrievingRepositoryMetadataAndJGitNotInit(Class<? extends Exception> exceptionClass) throws JGitSupplier.JGitInitializationException {
    var expectedException = new JGitSupplier.JGitInitializationException(mock(exceptionClass));
    var jgitSupplier = mock(JGitSupplier.class);
    when(jgitSupplier.getGit(any())).thenThrow(expectedException);
    var gitService = new JGitService(BASE_DIR_PLACEHOLDER, jgitSupplier);

    var gitResult = gitService.retrieveRepositoryMetadata();
    gitService.close();

    assertThat(gitResult).isEqualTo(GitService.RepositoryMetadataResult.UNSUCCESSFUL);
    assertThat(logTester.logs())
      .anySatisfy(line -> assertThat(line).contains("Exception querying Git data: " + expectedException.getMessage()));
  }

  @Test
  void shouldReturnUnsuccessfulStatusWhenRetrievingRepositoryMetadataFails() throws GitAPIException {
    var git = mock(Git.class);
    var remoteList = mock(RemoteListCommand.class);
    when(git.remoteList()).thenReturn(remoteList);
    var expectedException = new CanceledException("canceled");
    when(remoteList.call()).thenThrow(expectedException);

    var jgitSupplier = mock(JGitSupplier.class);
    when(jgitSupplier.getGit(any())).thenReturn(git);

    var gitService = new JGitService(BASE_DIR_PLACEHOLDER, jgitSupplier);
    var gitResult = gitService.retrieveRepositoryMetadata();
    gitService.close();

    assertThat(gitResult).isEqualTo(GitService.RepositoryMetadataResult.UNSUCCESSFUL);
    assertThat(logTester.logs())
      .anySatisfy(line -> assertThat(line).contains("Exception querying Git data: " + expectedException.getMessage()));
  }

  @Test
  void shouldReturnUnsuccessfulStatusWhenRetrievingRepositoryMetadataAndRemoteHasNoUri() {
    var git = mock(Git.class);
    var remoteList = mock(RemoteListCommand.class);
    when(git.remoteList()).thenReturn(remoteList);

    var jgitSupplier = mock(JGitSupplier.class);
    when(jgitSupplier.getGit(any())).thenReturn(git);

    var gitService = spy(new JGitService(BASE_DIR_PLACEHOLDER, jgitSupplier));
    var mockRemote = mock(RemoteConfig.class);
    when(mockRemote.getURIs()).thenReturn(Collections.emptyList());
    when(gitService.getRemotes()).thenReturn(Collections.singletonList(mockRemote));

    var gitResult = gitService.retrieveRepositoryMetadata();
    gitService.close();

    assertThat(gitResult).isEqualTo(GitService.RepositoryMetadataResult.UNSUCCESSFUL);
  }

  @ParameterizedTest
  @MethodSource
  void shouldReturnUnsuccessfulWhenRetrievingRepositoryMetadataAndRemoteIsMalformed(String malformedRemoteUri) {
    var git = setupGitMockWithRemote(malformedRemoteUri);

    var jgitSupplierMock = mock(JGitSupplier.class);
    when(jgitSupplierMock.getGit(any())).thenReturn(git);

    try (var gitService = new JGitService(BASE_DIR_PLACEHOLDER, jgitSupplierMock)) {
      var result = gitService.retrieveRepositoryMetadata();
      assertThat(result).isEqualTo(GitService.RepositoryMetadataResult.UNSUCCESSFUL);
      assertThat(logTester.logs())
        .anySatisfy(line -> assertThat(line).contains("Failed to parse repository metadata from remote"));
    }
  }

  static Stream<String> shouldReturnUnsuccessfulWhenRetrievingRepositoryMetadataAndRemoteIsMalformed() {
    return Stream.of(
      "", // Empty URI
      "/", // Single slash, no organization or project name
      "/org/", // Organization name only, no project name
      "/project.git", // Project name only, no organization name
      "/org//project.git" // Double slash in the path
    );
  }

  private static Git setupGitMockWithRemote(String remoteUri) {
    var git = mock(Git.class);

    var remoteListCommandMock = mock(RemoteListCommand.class);
    when(git.remoteList()).thenReturn(remoteListCommandMock);
    var remoteConfigMock = mock(RemoteConfig.class);
    var uriMock = mock(URIish.class);
    when(uriMock.getRawPath()).thenReturn(remoteUri);
    when(remoteConfigMock.getURIs()).thenReturn(Collections.singletonList(uriMock));
    try {
      when(remoteListCommandMock.call()).thenReturn(Collections.singletonList(remoteConfigMock));
    } catch (GitAPIException e) {
      throw new RuntimeException(e);
    }

    return git;
  }
}
