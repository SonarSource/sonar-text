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
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.InputFile;

public class GitTrackedFilePredicate implements FilePredicate {
  private static final Logger LOG = LoggerFactory.getLogger(GitTrackedFilePredicate.class);
  private final Set<String> ignoredFileNames = new HashSet<>();
  private final Set<String> untrackedFileNames;
  private final boolean isGitStatusSuccessful;
  private final Path projectRootPath;
  private final FilePredicate defaultFilePredicate;

  public GitTrackedFilePredicate(Path baseDir, GitService gitService, FilePredicate defaultFilePredicate) {
    var gitResult = gitService.retrieveUntrackedFileNames();
    this.untrackedFileNames = gitResult.untrackedFileNames();
    this.isGitStatusSuccessful = gitResult.isGitSuccessful();
    this.projectRootPath = baseDir;
    if (!isGitStatusSuccessful) {
      LOG.debug("Unable to retrieve git status");
    }
    this.defaultFilePredicate = defaultFilePredicate;
  }

  @Override
  public boolean apply(InputFile inputFile) {
    if (isGitStatusSuccessful) {
      var filePath = Path.of(inputFile.uri()).toAbsolutePath();
      String relativePath;
      try {
        relativePath = projectRootPath.relativize(filePath).toString();
      } catch (IllegalArgumentException e) {
        LOG.debug("Unable to resolve git status for {}, falling back to analyzing the file if it's associated with a language", inputFile, e);
        return defaultFilePredicate.apply(inputFile);
      }
      var result = !untrackedFileNames.contains(relativePath);
      if (!result) {
        ignoredFileNames.add(relativePath);
      }
      return result;
    } else {
      return true;
    }
  }

  public boolean isGitStatusSuccessful() {
    return isGitStatusSuccessful;
  }

  public void logSummary() {
    var numberOfIgnoredFiles = ignoredFileNames.size();
    if (numberOfIgnoredFiles > 0) {
      if (numberOfIgnoredFiles == 1) {
        LOG.info("1 file is ignored because it is untracked by git");
      } else {
        LOG.info("{} files are ignored because they are untracked by git", numberOfIgnoredFiles);
      }
      var fileList = ignoredFileNames.stream().sorted().collect(Collectors.joining("\n\t"));
      LOG.debug("Files untracked by git:\n\t{}", fileList);
    }
  }
}
