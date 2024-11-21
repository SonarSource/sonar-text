/*
 * SonarQube Text Plugin
 * Copyright (C) 2021-2024 SonarSource SA
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
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.plugins.common.git.utils.ProcessBuilderWrapper;

public class GitService {
  private static final Logger LOG = LoggerFactory.getLogger(GitService.class);
  private static final String GIT_PORCELAIN_UNTRACKED_FILE_MARKER = "??";
  private static final String WHERE_EXE_LOCATION = System.getenv("systemroot") + "\\System32\\where.exe";
  private final JgitSupplier jgitSupplier;
  private final ProcessBuilderWrapper processBuilderWrapper;
  private String gitCommand = "git";

  public GitService() {
    this(new JgitSupplier(), new ProcessBuilderWrapper());
  }

  public GitService(JgitSupplier jgitSupplier, ProcessBuilderWrapper processBuilderWrapper) {
    this.jgitSupplier = jgitSupplier;
    this.processBuilderWrapper = processBuilderWrapper;
  }

  public Result retrieveUntrackedFileNames(Path baseDir) {
    try {
      if (isGitCliAvailable()) {
        LOG.info("Using git CLI to retrieve untracked files");
        return getUntrackedFilesFromGitCli();
      } else {
        LOG.info("Using JGit to retrieve untracked files");
        return getUntrackedFilesFromJgit(jgitSupplier, baseDir);
      }
    } catch (JgitSupplier.JgitInitializationException | GitAPIException | IOException e) {
      LOG.debug("Exception querying Git data: {}", e.getMessage());
      return new Result(false, Set.of());
    }
  }

  boolean isGitCliAvailable() throws IOException {
    if (isWindows()) {
      gitCommand = Objects.requireNonNullElse(locateGitOnWindows(), gitCommand);
    }

    try {
      var status = execute(List.of(gitCommand, "--version"), (String line) -> LOG.debug("git --version returned: {}", line));
      return status == ProcessBuilderWrapper.Status.SUCCESS;
    } catch (IOException e) {
      LOG.debug("Not using Git CLI, `git --version` failed: {}", e.getMessage());
    }
    return false;
  }

  private Result getUntrackedFilesFromGitCli() throws IOException {
    Set<String> untrackedFiles = ConcurrentHashMap.newKeySet();

    try {
      var status = execute(List.of(gitCommand, "status", "--untracked-files=all", "--porcelain"), (String line) -> {
        if (line.startsWith(GIT_PORCELAIN_UNTRACKED_FILE_MARKER)) {
          untrackedFiles.add(line.substring(GIT_PORCELAIN_UNTRACKED_FILE_MARKER.length()).trim());
        }
      });
      if (status != ProcessBuilderWrapper.Status.SUCCESS) {
        return new Result(false, Set.of());
      }
    } catch (IOException e) {
      return new Result(false, Set.of());
    }

    return new Result(true, untrackedFiles);
  }

  String locateGitOnWindows() throws IOException {
    // Windows will search current directory in addition to the PATH variable, which is unsecure.
    // To avoid it we use where.exe to find git binary only in PATH.
    var whereResultLines = new LinkedList<String>();
    var gitExecutableName = "git.exe";

    execute(List.of(WHERE_EXE_LOCATION, "$PATH:" + gitExecutableName), whereResultLines::add);

    if (!whereResultLines.isEmpty()) {
      var whereResult = whereResultLines.get(0).trim();
      LOG.debug("Found {} at {}", gitExecutableName, whereResult);
      return whereResult;
    }
    if (LOG.isDebugEnabled()) {
      LOG.debug("{} not found in PATH. PATH value was: {}", gitExecutableName, System.getenv("PATH"));
    }
    return null;
  }

  private static Result getUntrackedFilesFromJgit(JgitSupplier jgitSupplier, Path baseDir) throws GitAPIException {
    try (var git = jgitSupplier.getGit(baseDir)) {
      var status = git.status().call();
      return new Result(true, status.getUntracked());
    }
  }

  ProcessBuilderWrapper.Status execute(List<String> command, Consumer<String> lineConsumer) throws IOException {
    return processBuilderWrapper.execute(command, lineConsumer);
  }

  static boolean isWindows() {
    return System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("windows");
  }

  public record Result(boolean isGitStatusSuccessful, Set<String> untrackedFileNames) {
  }
}
