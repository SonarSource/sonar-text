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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GitCliAndJGitService extends GitService {
  private static final Logger LOG = LoggerFactory.getLogger(GitCliAndJGitService.class);
  private final GitCliService gitCliService;
  private final JGitService jGitService;

  public GitCliAndJGitService(Path baseDir) {
    this(GitCliService.createOsSpecificInstance(baseDir), new JGitService(baseDir));
  }

  // Visible for testing
  GitCliAndJGitService(GitCliService gitCliService, JGitService jGitService) {
    this.gitCliService = gitCliService;
    this.jGitService = jGitService;
  }

  @Override
  public UntrackedFileNamesResult retrieveUntrackedFileNames() {
    if (gitCliService.isAvailable()) {
      LOG.info("Using Git CLI to retrieve untracked files");
      return gitCliService.retrieveUntrackedFileNames();
    }
    LOG.info("Using JGit to retrieve untracked files");
    return jGitService.retrieveUntrackedFileNames();
  }

  @Override
  public RepositoryMetadataResult retrieveRepositoryMetadata() {
    if (gitCliService.isAvailable()) {
      LOG.info("Using Git CLI to retrieve repository metadata");
      return gitCliService.retrieveRepositoryMetadata();
    }
    LOG.info("Using JGit to retrieve repository metadata");
    return jGitService.retrieveRepositoryMetadata();
  }

  @Override
  public void close() {
    gitCliService.close();
    jGitService.close();
  }
}
