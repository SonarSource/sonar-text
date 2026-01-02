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

import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.RemoteConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JGitService extends GitService {

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
  public DirtyFileNamesResult retrieveDirtyFileNames() {
    try (var git = jGitSupplier.getGit(baseDir)) {
      var status = git.status().call();
      var dirtyFiles = new HashSet<String>();
      dirtyFiles.addAll(status.getUntracked());
      dirtyFiles.addAll(status.getModified());
      dirtyFiles.addAll(status.getChanged());
      return new DirtyFileNamesResult(true, dirtyFiles);
    } catch (JGitSupplier.JGitInitializationException | GitAPIException e) {
      LOG.debug("Exception querying Git data: {}", e.getMessage());
      return DirtyFileNamesResult.UNSUCCESSFUL;
    }
  }

  @Override
  public RepositoryMetadataResult retrieveRepositoryMetadata() {
    List<RemoteConfig> remotes = getRemotes();
    if (remotes.isEmpty()) {
      return RepositoryMetadataResult.UNSUCCESSFUL;
    }

    var defaultRemoteURIs = remotes.get(0).getURIs();
    if (defaultRemoteURIs.isEmpty()) {
      return RepositoryMetadataResult.UNSUCCESSFUL;
    }

    var defaultRemoteUri = defaultRemoteURIs.get(0).getRawPath();
    return parseRepositoryMetadataFromRemoteUri(defaultRemoteUri);
  }

  // Visible for testing
  List<RemoteConfig> getRemotes() {
    try (var git = jGitSupplier.getGit(baseDir)) {
      return git.remoteList().call();
    } catch (JGitSupplier.JGitInitializationException | GitAPIException e) {
      LOG.debug("Exception querying Git data: {}", e.getMessage());
      return List.of();
    }
  }

  @Override
  public void close() {
    // No resources to close
  }
}
