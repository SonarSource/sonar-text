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
package org.sonar.plugins.secrets.configuration.model.matching.filter;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import java.util.Collections;
import java.util.List;

public class FileFilter {
  @JsonSetter(nulls = Nulls.SKIP)
  private List<String> paths = Collections.emptyList();
  @JsonSetter(nulls = Nulls.SKIP)
  private List<String> ext = Collections.emptyList();
  @JsonSetter(nulls = Nulls.SKIP)
  private List<String> content = Collections.emptyList();

  public List<String> getPaths() {
    return paths;
  }

  public void setPaths(List<String> paths) {
    this.paths = paths;
  }

  public List<String> getExt() {
    return ext;
  }

  public void setExt(List<String> ext) {
    this.ext = ext;
  }

  public List<String> getContent() {
    return content;
  }

  public void setContent(List<String> content) {
    this.content = content;
  }
}
