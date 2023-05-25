/*
 * SonarQube Text Plugin
 * Copyright (C) 2021-2023 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.plugins.secrets.configuration.model.metadata;

import java.util.List;
import javax.annotation.Nullable;

public abstract class Metadata {

  private String name;
  @Nullable
  private String message;
  @Nullable
  private List<Reference> references;
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

  @Nullable
  public String getMessage() {
    return message;
  }

  public void setMessage(@Nullable String message) {
    this.message = message;
  }

  @Nullable
  public List<Reference> getReferences() {
    return references;
  }

  public void setReferences(@Nullable List<Reference> references) {
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
