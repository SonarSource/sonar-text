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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.plugins.common.git.utils.ProcessBuilderWrapper;

public class GitService {
  private static final Logger LOG = LoggerFactory.getLogger(GitService.class);
  private static final String GIT_PORCELAIN_UNTRACKED_FILE_MARKER = "??";
  private final JgitSupplier gitSupplier;
  private String gitCommand = "git";

  public GitService() {
    this(new JgitSupplier());
  }

  public GitService(JgitSupplier gitSupplier) {
    this.gitSupplier = gitSupplier;
  }

  public Result retrieveUntrackedFileNames() {
    try {
      if (isGitCliAvailable()) {
        LOG.debug("Using git CLI to retrieve untracked files");
        return getUntrackedFilesFromGitCli();
      } else {
        LOG.debug("Using JGit to retrieve untracked files");
        return getUntrackedFilesFromJgit(gitSupplier);
      }
    } catch (JgitSupplier.JgitInitializationException | GitAPIException | IOException e) {
      LOG.debug("Unable to retrieve git status", e);
      return new Result(false, Set.of());
    }
  }

  boolean isGitCliAvailable() throws IOException {
    if (System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("windows")) {
      gitCommand = Objects.requireNonNullElse(locateGitOnWindows(), gitCommand);
    }

    var pbw = getGitProcessBuilder(List.of("--version"));
    try {
      var status = pbw.execute((String line) -> LOG.debug("git --version returned: {}", line));
      return status == ProcessBuilderWrapper.Status.SUCCESS;
    } catch (IOException e) {
      LOG.debug("Not using Git CLI, `git --version` failed: {}", e.getMessage());
    }
    return false;
  }

  private Result getUntrackedFilesFromGitCli() throws IOException {
    var untrackedFiles = new HashSet<String>();

    var wrapper = getGitProcessBuilder(List.of("status", "--untracked-files=all", "--porcelain"));
    try {
      var status = wrapper.execute((String line) -> {
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

  static String locateGitOnWindows() throws IOException {
    // Windows will search current directory in addition to the PATH variable, which is unsecure.
    // To avoid it we use where.exe to find git binary only in PATH.
    var whereResultLines = new LinkedList<String>();
    var gitExecutableName = "git.exe";

    var wrapper = new ProcessBuilderWrapper(List.of("C:\\Windows\\System32\\where.exe", "$PATH:" + gitExecutableName));
    wrapper.execute(whereResultLines::add);

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

  private static Result getUntrackedFilesFromJgit(JgitSupplier jgitSupplier) throws GitAPIException {
    try (var git = jgitSupplier.getGit()) {
      var status = git.status().call();
      return new Result(true, status.getUntracked());
    }
  }

  ProcessBuilderWrapper getGitProcessBuilder(List<String> gitArgs) {
    var command = new ArrayList<String>();
    command.add(gitCommand);
    command.addAll(gitArgs);
    return new ProcessBuilderWrapper(command);
  }

  public record Result(boolean isGitStatusSuccessful, Set<String> untrackedFileNames) {
  }
}
