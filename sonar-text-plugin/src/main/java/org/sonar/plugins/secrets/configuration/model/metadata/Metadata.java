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
package org.sonar.plugins.secrets.configuration.model.metadata;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;

public abstract class Metadata {

  private String name;
  @Nullable
  private String message;
  @JsonSetter(nulls = Nulls.SKIP)
  private List<Reference> references = Collections.emptyList();
  @Nullable
  private String impact;
  @Nullable
  private String fix;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(@Nullable String message) {
    this.message = message;
  }

  public List<Reference> getReferences() {
    return references;
  }

  public void setReferences(List<Reference> references) {
    this.references = references;
  }

  @Nullable
  public String getImpact() {
    return impact;
  }

  public void setImpact(@Nullable String impact) {
    this.impact = impact;
  }

  @Nullable
  public String getFix() {
    return fix;
  }

  public void setFix(@Nullable String fix) {
    this.fix = fix;
  }
}
