/*
 * SonarQube Text Plugin
 * Copyright (C) 2021-2026 SonarSource Sàrl
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

import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class GitService implements AutoCloseable {
  private static final Logger LOG = LoggerFactory.getLogger(GitService.class);

  public abstract DirtyFileNamesResult retrieveDirtyFileNames();

  public abstract RepositoryMetadataResult retrieveRepositoryMetadata();

  /**
   * Extracts repository metadata (organization and project name) from a remote URI.
   * Supported URI formats include:
   * <ul>
   *   <li>/org/project.git</li>
   *   <li>/org/project</li>
   *   <li>https://github.com/org/project.git</li>
   *   <li>git@github.com:org/project.git</li>
   * </ul>
   */
  protected static RepositoryMetadataResult parseRepositoryMetadataFromRemoteUri(String remoteUri) {
    try {
      var remoteUriParts = remoteUri.split("[/:]");

      var repositoryName = remoteUriParts[remoteUriParts.length - 1];
      var extensionIndex = repositoryName.lastIndexOf('.');
      if (extensionIndex != -1) {
        repositoryName = repositoryName.substring(0, extensionIndex);
      }
      var organizationName = remoteUriParts[remoteUriParts.length - 2];

      if (repositoryName.isEmpty() || organizationName.isEmpty()) {
        LOG.debug("Failed to parse repository metadata from remote '{}'", remoteUri);
        return RepositoryMetadataResult.UNSUCCESSFUL;
      }

      return new RepositoryMetadataResult(true, repositoryName, organizationName);
    } catch (Exception e) {
      LOG.debug("Failed to parse repository metadata from remote '{}': {}", remoteUri, e.getMessage());
      return RepositoryMetadataResult.UNSUCCESSFUL;
    }
  }

  public record DirtyFileNamesResult(boolean isGitSuccessful, Set<String> dirtyFileNames) {
    public static final DirtyFileNamesResult UNSUCCESSFUL = new DirtyFileNamesResult(false, Set.of());
  }

  public record RepositoryMetadataResult(boolean isGitSuccessful, String projectName, String organizationName) {
    public static final RepositoryMetadataResult UNSUCCESSFUL = new RepositoryMetadataResult(false, "", "");
  }
}
