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
package org.sonar.plugins.secrets.configuration.model.matching.filter;

import java.util.List;
import javax.annotation.Nullable;

public class IncludedFilter {

  @Nullable
  private List<String> paths;
  @Nullable
  private List<String> ext;
  @Nullable
  private List<String> content;

  @Nullable
  public List<String> getPaths() {
    return paths;
  }

  public void setPaths(@Nullable List<String> paths) {
    this.paths = paths;
  }

  @Nullable
  public List<String> getExt() {
    return ext;
  }

  public void setExt(@Nullable List<String> ext) {
    this.ext = ext;
  }

  @Nullable
  public List<String> getContent() {
    return content;
  }

  public void setContent(@Nullable List<String> content) {
    this.content = content;
  }
}
