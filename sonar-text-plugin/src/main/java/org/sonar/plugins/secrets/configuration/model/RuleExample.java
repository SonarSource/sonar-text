/*
 * SonarQube Text Plugin
 * Copyright (C) 2021-2024 SonarSource SA
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
package org.sonar.plugins.secrets.configuration.model;

import javax.annotation.Nullable;

public class RuleExample {

  private String text;
  private boolean containsSecret;
  @Nullable
  private String match;

  @Nullable
  private String fileName;

  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
  }

  public boolean isContainsSecret() {
    return containsSecret;
  }

  public void setContainsSecret(boolean containsSecret) {
    this.containsSecret = containsSecret;
  }

  @Nullable
  public String getMatch() {
    return match;
  }

  public void setMatch(@Nullable String match) {
    this.match = match;
  }

  @Nullable
  public String getFileName() {
    return fileName;
  }

  public void setFileName(@Nullable String fileName) {
    this.fileName = fileName;
  }
}
