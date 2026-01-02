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

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.URIish;

/**
 * Fluent builder for creating test Git repositories with a clean, declarative API.
 * Handles all resource management and exception handling internally.
 *
 * <p>Example usage:
 * <pre>
 * GitRepoBuilder.builder(tempDir)
 *   .withTrackedFile("file1.txt", "file2.txt")
 *   .withUntrackedFile("untracked.txt")
 *   .withGitHubRemote("org", "project")
 *   .build();
 * </pre>
 */
public class GitRepoBuilder {
  private final Path repoPath;
  private final List<String> trackedFiles = new ArrayList<>();
  private final List<String> untrackedFiles = new ArrayList<>();
  private final List<String> unstagedModifiedFiles = new ArrayList<>();
  private final List<String> stagedModifiedFiles = new ArrayList<>();
  private final List<String> stagedThenModifiedFiles = new ArrayList<>();
  private String remoteName;
  private String remoteUrl;

  private GitRepoBuilder(Path repoPath) {
    this.repoPath = repoPath;
  }

  /**
   * Start building a git repository at the specified path.
   *
   * @param path The directory where the repository will be created
   * @return A new GitRepoBuilder instance
   */
  public static GitRepoBuilder builder(Path path) {
    return new GitRepoBuilder(path);
  }

  /**
   * Creates a clean git repository with a single tracked file.
   * This is a convenience method for the common case of needing a minimal valid repository.
   *
   * @param repoPath The directory where the repository will be created
   */
  public static void setupCleanRepo(Path repoPath) {
    builder(repoPath)
      .withTrackedFile("initial.txt")
      .build();
  }

  /**
   * Creates a git repository with a tracked file and a configured GitHub remote.
   * This is a convenience method for tests that need repository metadata.
   *
   * @param repoPath The directory where the repository will be created
   * @param organizationName The organization name for the remote
   * @param projectName The project name for the remote
   */
  public static void setupRepoWithRemote(Path repoPath, String organizationName, String projectName) {
    builder(repoPath)
      .withTrackedFile("initial.txt")
      .withGitHubRemote(organizationName, projectName)
      .build();
  }

  /**
   * Add tracked files to the repository. Files will be committed.
   *
   * @param files One or more file names to create and track
   * @return This builder for chaining
   */
  public GitRepoBuilder withTrackedFile(String... files) {
    trackedFiles.addAll(Arrays.asList(files));
    return this;
  }

  /**
   * Add untracked files to the repository. Files will be created but not committed.
   *
   * @param files One or more file names to create as untracked
   * @return This builder for chaining
   */
  public GitRepoBuilder withUntrackedFile(String... files) {
    untrackedFiles.addAll(Arrays.asList(files));
    return this;
  }

  /**
   * Add unstaged modified files to the repository. Files will be tracked, committed, then modified
   * but not staged. This produces the " M" status in git porcelain output.
   *
   * @param files One or more file names to create as tracked then modify (unstaged)
   * @return This builder for chaining
   */
  public GitRepoBuilder withUnstagedModifiedFile(String... files) {
    unstagedModifiedFiles.addAll(Arrays.asList(files));
    return this;
  }

  /**
   * Add staged modified files to the repository. Files will be tracked, committed, modified,
   * then staged. This produces the "M " status in git porcelain output.
   *
   * @param files One or more file names to create as tracked, modify, then stage
   * @return This builder for chaining
   */
  public GitRepoBuilder withStagedModifiedFile(String... files) {
    stagedModifiedFiles.addAll(Arrays.asList(files));
    return this;
  }

  /**
   * Add staged then modified files to the repository. Files will be tracked, committed, modified,
   * staged, then modified again. This produces the "MM" status in git porcelain output.
   *
   * @param files One or more file names to create with both staged and unstaged modifications
   * @return This builder for chaining
   */
  public GitRepoBuilder withStagedThenModifiedFile(String... files) {
    stagedThenModifiedFiles.addAll(Arrays.asList(files));
    return this;
  }

  /**
   * Configure a GitHub remote for the repository.
   *
   * @param orgName The organization name
   * @param projectName The project name
   * @return This builder for chaining
   */
  public GitRepoBuilder withGitHubRemote(String orgName, String projectName) {
    var githubUrl = String.format("https://github.com/%s/%s.git", orgName, projectName);
    return withRemote("origin", githubUrl);
  }

  /**
   * Configure a custom remote for the repository.
   *
   * @param remoteName The name of the remote (e.g., "origin")
   * @param remoteUrl The URL of the remote
   * @return This builder for chaining
   */
  public GitRepoBuilder withRemote(String remoteName, String remoteUrl) {
    this.remoteName = remoteName;
    this.remoteUrl = remoteUrl;
    return this;
  }

  /**
   * Build the git repository with the configured settings.
   * All resources are properly managed and closed.
   *
   * @throws RuntimeException if repository setup fails
   */
  public void build() {
    try (var git = initGitRepo(repoPath)) {
      commitAllTrackedFiles(git);

      for (var file : unstagedModifiedFiles) {
        modifyFile(repoPath, file);
      }
      for (var file : stagedModifiedFiles) {
        modifyFile(repoPath, file);
        stageFile(git, file);
      }
      for (var file : stagedThenModifiedFiles) {
        modifyFile(repoPath, file);
        stageFile(git, file);
        modifyFile(repoPath, file);
      }

      if (remoteName != null && remoteUrl != null) {
        configureRemote(git, remoteName, remoteUrl);
      }
    } catch (Exception e) {
      throw new RuntimeException("Failed to set up git repository", e);
    }

    // Create untracked files after closing Git (to avoid auto-tracking)
    try {
      for (var file : untrackedFiles) {
        createFile(repoPath, file);
      }
    } catch (Exception e) {
      throw new RuntimeException("Failed to create untracked files", e);
    }
  }

  private static Git initGitRepo(Path directory) throws GitAPIException, IOException {
    var git = Git.init().setDirectory(directory.toFile()).call();
    // Configure git user for commits
    var config = git.getRepository().getConfig();
    config.setString("user", null, "name", "Test User");
    config.setString("user", null, "email", "test@example.com");
    // Explicitly disable commit signing to avoid crashes when the environment's git config uses SSH
    config.setBoolean("commit", null, "gpgsign", false);
    config.save();
    return git;
  }

  private void commitAllTrackedFiles(Git git) throws IOException, GitAPIException {
    var allFiles = new ArrayList<String>();
    allFiles.addAll(trackedFiles);
    allFiles.addAll(unstagedModifiedFiles);
    allFiles.addAll(stagedModifiedFiles);
    allFiles.addAll(stagedThenModifiedFiles);

    if (allFiles.isEmpty()) {
      return;
    }

    for (var file : allFiles) {
      createFile(repoPath, file);
      stageFile(git, file);
    }
    git.commit().setMessage("Initial commit").call();
  }

  private static void createFile(Path repoPath, String fileName) throws IOException {
    var filePath = repoPath.resolve(fileName);
    var parentDir = filePath.getParent();
    if (parentDir != null) {
      Files.createDirectories(parentDir);
    }
    Files.writeString(filePath, "test content");
  }

  private static void modifyFile(Path repoPath, String fileName) throws IOException {
    var filePath = repoPath.resolve(fileName);
    Files.writeString(filePath, UUID.randomUUID().toString());
  }

  private static void stageFile(Git git, String file) throws GitAPIException {
    git.add().addFilepattern(file).call();
  }

  private static void configureRemote(Git git, String remoteName, String remoteUrl) throws GitAPIException, URISyntaxException {
    git.remoteAdd()
      .setName(remoteName)
      .setUri(new URIish(remoteUrl))
      .call();
  }
}
