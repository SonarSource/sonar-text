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
package org.sonar.plugins.secrets.configuration.model.matching.filter;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import org.sonar.plugins.secrets.configuration.model.RuleScope;

public class PreModule {

  @Nullable
  private FileFilter include;
  @Nullable
  private FileFilter reject;
  @JsonSetter(nulls = Nulls.SKIP)
  private List<RuleScope> scopes = Collections.emptyList();

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

  public List<RuleScope> getScopes() {
    return scopes;
  }

  public void setScopes(List<RuleScope> scopes) {
    this.scopes = scopes;
  }
}
