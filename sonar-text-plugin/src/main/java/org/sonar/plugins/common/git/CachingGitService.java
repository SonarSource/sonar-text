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

import javax.annotation.Nullable;

/**
 * A proxy of {@link GitService} that caches the results of the underlying service.
 */
public class CachingGitService extends GitService {

  private final GitService underlyingService;

  @Nullable
  private GitService.DirtyFileNamesResult dirtyFileNamesResult;

  @Nullable
  private GitService.RepositoryMetadataResult repositoryMetadataResult;

  public CachingGitService(GitService underlyingService) {
    this.underlyingService = underlyingService;
  }

  @Override
  public synchronized GitService.DirtyFileNamesResult retrieveDirtyFileNames() {
    if (dirtyFileNamesResult == null) {
      dirtyFileNamesResult = underlyingService.retrieveDirtyFileNames();
    }
    return dirtyFileNamesResult;
  }

  @Override
  public synchronized GitService.RepositoryMetadataResult retrieveRepositoryMetadata() {
    if (repositoryMetadataResult == null) {
      repositoryMetadataResult = underlyingService.retrieveRepositoryMetadata();
    }
    return repositoryMetadataResult;
  }

  @Override
  public void close() throws Exception {
    underlyingService.close();
  }
}
