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
package org.sonar.plugins.common;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.InputFile;

public class GitTrackedFilePredicate implements FilePredicate {
  private static final Logger LOG = LoggerFactory.getLogger(GitTrackedFilePredicate.class);
  private Set<String> untrackedFileNames;
  private Git git;
  private boolean isGitStatusSuccessful;
  private Path projectRootPath;

  public GitTrackedFilePredicate(GitSupplier gitSupplier) {
    try {
      this.git = gitSupplier.getGit();
      var status = git.status().call();
      this.untrackedFileNames = status.getUntracked();
      isGitStatusSuccessful = true;
    } catch (GitAPIException | IOException | RuntimeException e) {
      this.untrackedFileNames = Set.of();
      isGitStatusSuccessful = false;
      LOG.debug("Unable to retrieve Git status, won't perform any exclusions", e);
    } finally {
      if (this.git != null) {
        this.git.close();
      }
    }
    this.projectRootPath = Path.of(".").toAbsolutePath();
    try {
      projectRootPath = projectRootPath.toRealPath();
    } catch (IOException e) {
      var message = String.format("Unable to resolve real path of project for %s", projectRootPath);
      LOG.debug(message, e);
    }
  }

  @Override
  public boolean apply(InputFile inputFile) {
    if (isGitStatusSuccessful) {
      var filePath = Path.of(inputFile.uri()).toAbsolutePath();
      var relativePath = projectRootPath.relativize(filePath).toString();
      return !untrackedFileNames.contains(relativePath);
    } else {
      return true;
    }
  }

  public boolean isGitStatusSuccessful() {
    return isGitStatusSuccessful;
  }
}
