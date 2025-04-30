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
import java.util.Set;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JGitService implements GitService {

  private static final Logger LOG = LoggerFactory.getLogger(JGitService.class);
  private final Path baseDir;
  private final JGitSupplier jGitSupplier;

  public JGitService(Path baseDir) {
    this(baseDir, new JGitSupplier());
  }

  // Visible for testing
  JGitService(Path baseDir, JGitSupplier jGitSupplier) {
    this.baseDir = baseDir;
    this.jGitSupplier = jGitSupplier;
  }

  @Override
  public UntrackedFileNamesResult retrieveUntrackedFileNames() {
    try (var git = jGitSupplier.getGit(baseDir)) {
      var status = git.status().call();
      return new UntrackedFileNamesResult(true, status.getUntracked());
    } catch (JGitSupplier.JGitInitializationException | GitAPIException e) {
      LOG.debug("Exception querying Git data: {}", e.getMessage());
      return new UntrackedFileNamesResult(false, Set.of());
    }
  }

  @Override
  public void close() {
    // No resources to close
  }
}
