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
import java.nio.file.Path;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryBuilder;

public class JgitSupplier {
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
      throw new JgitInitializationException(e);
    }
    return new Git(repository);
  }

  public static class JgitInitializationException extends RuntimeException {
    public JgitInitializationException(Throwable cause) {
      super(cause);
    }
  }
}
