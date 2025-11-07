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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class NullGitService extends GitService {

  public static final NullGitService INSTANCE = new NullGitService();
  private static final Logger LOG = LoggerFactory.getLogger(NullGitService.class);

  private NullGitService() {
    // Private constructor to prevent instantiation
  }

  @Override
  public GitService.UntrackedFileNamesResult retrieveUntrackedFileNames() {
    logServiceNotInitialized();
    return GitService.UntrackedFileNamesResult.UNSUCCESSFUL;
  }

  @Override
  public RepositoryMetadataResult retrieveRepositoryMetadata() {
    logServiceNotInitialized();
    return GitService.RepositoryMetadataResult.UNSUCCESSFUL;
  }

  private static void logServiceNotInitialized() {
    LOG.debug("Git service has not been initialized, returning unsuccessful result");
  }

  @Override
  public void close() {
    // No resources to close
  }
}
