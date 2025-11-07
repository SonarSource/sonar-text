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

import java.io.IOException;
import java.nio.file.Path;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryBuilder;

public class JGitSupplier {
  public Git getGit(Path baseDir) {
    Repository repository;
    try {
      repository = new RepositoryBuilder()
        // scan environment GIT_* variables
        .readEnvironment()
        // scan up the file system tree
        .findGitDir(baseDir.toFile())
        .build();
    } catch (IOException | RuntimeException e) {
      // Jgit will throw RuntimeException when there is no git working-folder available
      throw new JGitInitializationException(e);
    }
    return new Git(repository);
  }

  public static class JGitInitializationException extends RuntimeException {
    public JGitInitializationException(Throwable cause) {
      super(cause);
    }
  }
}
