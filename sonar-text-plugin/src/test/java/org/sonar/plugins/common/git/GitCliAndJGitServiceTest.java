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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.sonar.plugins.common.TestUtils.inputFileFromPath;

class GitCliAndJGitServiceTest {

  @RegisterExtension
  LogTesterJUnit5 logTester = new LogTesterJUnit5();

  // Workaround to get the base directory of the project
  private static final Path BASE_DIR = inputFileFromPath(Paths.get("")).path();

  @Test
  void shouldRetrieveUntracked() {
    try (var gitService = new GitCliAndJGitService(BASE_DIR)) {
      var gitResult = gitService.retrieveUntrackedFileNames();
      assertThat(gitResult.isGitSuccessful()).isTrue();
      assertThat(gitResult.untrackedFileNames()).isEmpty();
    }
  }

  @Test
  void shouldRetrieveUntrackedFromCliWhenAvailable() {
    var gitCliService = spy(GitCliService.createOsSpecificInstance(BASE_DIR));
    when(gitCliService.retrieveUntrackedFileNames()).thenReturn(new GitService.UntrackedFileNamesResult(true, Set.of("a.txt")));
    var jGitService = new JGitService(BASE_DIR);
    try (var gitService = new GitCliAndJGitService(gitCliService, jGitService)) {
      var gitResult = gitService.retrieveUntrackedFileNames();
      assertThat(logTester.logs())
        .anySatisfy(line -> assertThat(line).contains("Using Git CLI to retrieve untracked files"));
      assertThat(gitResult.isGitSuccessful()).isTrue();
      assertThat(gitResult.untrackedFileNames()).contains("a.txt");
    }
  }

  @Test
  void shouldRetrieveUntrackedFromJGitWhenCliNotAvailable() {
    var gitCliService = spy(GitCliService.createOsSpecificInstance(BASE_DIR));
    when(gitCliService.isAvailable()).thenReturn(false);
    var jGitService = spy(new JGitService(BASE_DIR));
    when(jGitService.retrieveUntrackedFileNames()).thenReturn(new GitService.UntrackedFileNamesResult(true, Set.of("a.txt")));
    try (var gitService = new GitCliAndJGitService(gitCliService, jGitService)) {
      var gitResult = gitService.retrieveUntrackedFileNames();
      assertThat(logTester.logs())
        .anySatisfy(line -> assertThat(line).contains("Using JGit to retrieve untracked files"));
      assertThat(gitResult.isGitSuccessful()).isTrue();
      assertThat(gitResult.untrackedFileNames()).contains("a.txt");
    }
  }

  @Test
  void shouldRetrieveRepositoryMetadataFromCliWhenAvailable() {
    var gitCliService = spy(GitCliService.createOsSpecificInstance(BASE_DIR));
    when(gitCliService.retrieveRepositoryMetadata()).thenReturn(new GitService.RepositoryMetadataResult(true, "project", "org"));
    var jGitService = new JGitService(BASE_DIR);
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
    var gitCliService = spy(GitCliService.createOsSpecificInstance(BASE_DIR));
    when(gitCliService.isAvailable()).thenReturn(false);
    var jGitService = spy(new JGitService(BASE_DIR));
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
