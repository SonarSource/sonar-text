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

import java.util.function.Supplier;
import javax.annotation.Nullable;

/**
 * A proxy of {@link GitService} that lazily initializes the underlying service.
 */
public class LazyGitService extends GitService {

  private final Supplier<GitService> serviceSupplier;

  @Nullable
  private GitService serviceInstance;

  public LazyGitService(Supplier<GitService> serviceSupplier) {
    this.serviceSupplier = serviceSupplier;
  }

  @Override
  public UntrackedFileNamesResult retrieveUntrackedFileNames() {
    return getService().retrieveUntrackedFileNames();
  }

  @Override
  public RepositoryMetadataResult retrieveRepositoryMetadata() {
    return getService().retrieveRepositoryMetadata();
  }

  private GitService getService() {
    if (serviceInstance == null) {
      serviceInstance = serviceSupplier.get();
    }
    return serviceInstance;
  }

  @Override
  public void close() throws Exception {
    if (serviceInstance != null) {
      serviceInstance.close();
    }
  }
}
