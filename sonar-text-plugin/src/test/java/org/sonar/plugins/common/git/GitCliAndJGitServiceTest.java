/*
 * SonarQube Text Plugin
 * Copyright (C) 2021-2026 SonarSource Sàrl
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
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

class GitCliAndJGitServiceTest {

  @RegisterExtension
  LogTesterJUnit5 logTester = new LogTesterJUnit5();

  private static final Path BASE_DIR_PLACEHOLDER = Paths.get("fake-repo-dir");

  @Test
  void shouldRetrieveNoDirtyFilesFromCleanRepo(@TempDir Path tempDir) {
    GitRepoBuilder.setupCleanRepo(tempDir);

    try (var gitService = new GitCliAndJGitService(tempDir)) {
      var gitResult = gitService.retrieveDirtyFileNames();
      assertThat(gitResult.isGitSuccessful()).isTrue();
      // We expect an empty file list, but in the CI there may be additional files related to gradle build
      assertThat(gitResult.dirtyFileNames())
        .allMatch(f -> f.equals("build_number.txt") || f.equals("gradle.properties.bak") || f.equals("null/null/evaluated_project_version.txt"));
    }
  }

  @Test
  void shouldRetrieveUntrackedFilesFromRepo(@TempDir Path tempDir) {
    GitRepoBuilder.builder(tempDir)
      .withTrackedFile("tracked.txt")
      .withUntrackedFile("untracked1.txt", "untracked2.txt")
      .build();

    try (var gitService = new GitCliAndJGitService(tempDir)) {
      var gitResult = gitService.retrieveDirtyFileNames();
      assertThat(gitResult.isGitSuccessful()).isTrue();
      assertThat(gitResult.dirtyFileNames()).containsExactlyInAnyOrder("untracked1.txt", "untracked2.txt");
    }
  }

  @Test
  void shouldRetrieveDirtyFromCliWhenAvailable() {
    var gitCliService = spy(GitCliService.createOsSpecificInstance(BASE_DIR_PLACEHOLDER));
    when(gitCliService.retrieveDirtyFileNames()).thenReturn(new GitService.DirtyFileNamesResult(true, Set.of("a.txt")));
    var jGitService = new JGitService(BASE_DIR_PLACEHOLDER);
    try (var gitService = new GitCliAndJGitService(gitCliService, jGitService)) {
      var gitResult = gitService.retrieveDirtyFileNames();
      assertThat(logTester.logs())
        .anySatisfy(line -> assertThat(line).contains("Using Git CLI to retrieve dirty files"));
      assertThat(gitResult.isGitSuccessful()).isTrue();
      assertThat(gitResult.dirtyFileNames()).contains("a.txt");
    }
  }

  @Test
  void shouldRetrieveDirtyFromJGitWhenCliNotAvailable() {
    var gitCliService = spy(GitCliService.createOsSpecificInstance(BASE_DIR_PLACEHOLDER));
    when(gitCliService.isAvailable()).thenReturn(false);
    var jGitService = spy(new JGitService(BASE_DIR_PLACEHOLDER));
    when(jGitService.retrieveDirtyFileNames()).thenReturn(new GitService.DirtyFileNamesResult(true, Set.of("a.txt")));
    try (var gitService = new GitCliAndJGitService(gitCliService, jGitService)) {
      var gitResult = gitService.retrieveDirtyFileNames();
      assertThat(logTester.logs())
        .anySatisfy(line -> assertThat(line).contains("Using JGit to retrieve dirty files"));
      assertThat(gitResult.isGitSuccessful()).isTrue();
      assertThat(gitResult.dirtyFileNames()).contains("a.txt");
    }
  }

  @Test
  void shouldRetrieveRepositoryMetadataFromCliWhenAvailable() {
    var gitCliService = spy(GitCliService.createOsSpecificInstance(BASE_DIR_PLACEHOLDER));
    when(gitCliService.retrieveRepositoryMetadata()).thenReturn(new GitService.RepositoryMetadataResult(true, "project", "org"));
    var jGitService = new JGitService(BASE_DIR_PLACEHOLDER);
    try (var gitService = new GitCliAndJGitService(gitCliService, jGitService)) {
      var gitResult = gitService.retrieveRepositoryMetadata();
      assertThat(logTester.logs())
        .anySatisfy(line -> assertThat(line).contains("Using Git CLI to retrieve repository metadata"));
      assertThat(gitResult.isGitSuccessful()).isTrue();
      assertThat(gitResult.projectName()).isEqualTo("project");
      assertThat(gitResult.organizationName()).isEqualTo("org");
    }
  }

  @Test
  void shouldRetrieveRepositoryMetadataFromJGitWhenCliNotAvailable() {
    var gitCliService = spy(GitCliService.createOsSpecificInstance(BASE_DIR_PLACEHOLDER));
    when(gitCliService.isAvailable()).thenReturn(false);
    var jGitService = spy(new JGitService(BASE_DIR_PLACEHOLDER));
    when(jGitService.retrieveRepositoryMetadata()).thenReturn(new GitService.RepositoryMetadataResult(true, "project", "org"));
    try (var gitService = new GitCliAndJGitService(gitCliService, jGitService)) {
      var gitResult = gitService.retrieveRepositoryMetadata();
      assertThat(logTester.logs())
        .anySatisfy(line -> assertThat(line).contains("Using JGit to retrieve repository metadata"));
      assertThat(gitResult.isGitSuccessful()).isTrue();
      assertThat(gitResult.projectName()).isEqualTo("project");
      assertThat(gitResult.organizationName()).isEqualTo("org");
    }
  }
}
