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
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.plugins.common.git.utils.ProcessBuilderWrapper;

public final class GitCliService extends GitService {
  private static final Logger LOG = LoggerFactory.getLogger(GitCliService.class);
  private static final String GIT_PORCELAIN_UNTRACKED_FILE_MARKER = "??";
  private static final String WHERE_EXE_LOCATION = System.getenv("systemroot") + "\\System32\\where.exe";
  private String gitCommand = "git";
  private boolean available = false;

  private final Path baseDir;
  private final ProcessBuilderWrapper processBuilderWrapper;

  private GitCliService(Path baseDir, ProcessBuilderWrapper processBuilderWrapper) {
    this.baseDir = baseDir;
    this.processBuilderWrapper = processBuilderWrapper;
  }

  public static GitCliService createOsSpecificInstance(Path baseDir) {
    return createOsSpecificInstance(baseDir, new ProcessBuilderWrapper());
  }

  // Visible for testing
  static GitCliService createOsSpecificInstance(Path baseDir, ProcessBuilderWrapper processBuilderWrapper) {
    var instance = new GitCliService(baseDir, processBuilderWrapper);
    try {
      if (isWindows()) {
        var windowsGitCommand = instance.locateGitOnWindows();
        instance.gitCommand = Objects.requireNonNullElse(windowsGitCommand, instance.gitCommand);
      }
    } catch (IOException e) {
      LOG.debug("Failed to locate Git on Windows: {}", e.getMessage());
    }
    try {
      var gitVersionCommand = List.of(instance.gitCommand, "--version");
      var status = instance.execute(gitVersionCommand, (String line) -> LOG.debug("git --version returned: {}", line));
      instance.available = status == ProcessBuilderWrapper.Status.SUCCESS;
    } catch (IOException e) {
      LOG.debug("Not using Git CLI, `git --version` failed: {}", e.getMessage());
    }
    return instance;
  }

  @Override
  public UntrackedFileNamesResult retrieveUntrackedFileNames() {
    if (!available) {
      return UntrackedFileNamesResult.UNSUCCESSFUL;
    }
    Set<String> untrackedFiles = ConcurrentHashMap.newKeySet();

    try {
      var statusCommand = List.of(gitCommand, "-C", baseDir.toAbsolutePath().toString(), "status", "--untracked-files=all", "--porcelain");
      var status = execute(statusCommand, (String line) -> {
        if (line.startsWith(GIT_PORCELAIN_UNTRACKED_FILE_MARKER)) {
          untrackedFiles.add(line.substring(GIT_PORCELAIN_UNTRACKED_FILE_MARKER.length()).trim());
        }
      });
      if (status != ProcessBuilderWrapper.Status.SUCCESS) {
        return UntrackedFileNamesResult.UNSUCCESSFUL;
      }
    } catch (IOException e) {
      return UntrackedFileNamesResult.UNSUCCESSFUL;
    }

    return new UntrackedFileNamesResult(true, untrackedFiles);
  }

  @Override
  public RepositoryMetadataResult retrieveRepositoryMetadata() {
    if (!available) {
      return RepositoryMetadataResult.UNSUCCESSFUL;
    }

    List<String> remotes = getGitRemotes();
    if (remotes.isEmpty()) {
      return RepositoryMetadataResult.UNSUCCESSFUL;
    }

    var defaultRemote = remotes.get(0);
    return parseRepositoryMetadata(defaultRemote);
  }

  // Visible for testing
  List<String> getGitRemotes() {
    var remotes = new ArrayList<String>();
    try {
      var listRemotesCommand = List.of(gitCommand, "-C", baseDir.toAbsolutePath().toString(), "remote", "-v");
      var status = execute(listRemotesCommand, remotes::add);
      if (status != ProcessBuilderWrapper.Status.SUCCESS) {
        return List.of();
      }
    } catch (IOException e) {
      return List.of();
    }
    return remotes;
  }

  private static RepositoryMetadataResult parseRepositoryMetadata(String remote) {
    try {
      var remoteUrl = remote.split("\\s+")[1];
      return parseRepositoryMetadataFromRemoteUri(remoteUrl);
    } catch (Exception e) {
      LOG.debug("Failed to parse repository metadata from remote '{}': {}", remote, e.getMessage());
      return RepositoryMetadataResult.UNSUCCESSFUL;
    }
  }

  public boolean isAvailable() {
    return available;
  }

  // Visible for testing
  String getGitCommand() {
    return gitCommand;
  }

  // Visible for testing
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

  // Visible for testing
  ProcessBuilderWrapper.Status execute(List<String> command, Consumer<String> lineConsumer) throws IOException {
    return processBuilderWrapper.execute(command, lineConsumer);
  }

  // Visible for testing
  static boolean isWindows() {
    return System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("windows");
  }

  @Override
  public void close() {
    this.processBuilderWrapper.shutdown();
  }
}
