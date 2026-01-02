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
package org.sonar.plugins.secrets.configuration.model.matching.filter;

import javax.annotation.Nullable;

public class PreModule {

  @Nullable
  private FileFilter include;
  @Nullable
  private FileFilter reject;

  public PreModule() {
    // Default constructor for deserializing with Jackson
  }

  @Nullable
  public FileFilter getInclude() {
    return include;
  }

  public void setInclude(@Nullable FileFilter include) {
    this.include = include;
  }

  @Nullable
  public FileFilter getReject() {
    return reject;
  }

  public void setReject(@Nullable FileFilter reject) {
    this.reject = reject;
  }

}
