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

import com.fasterxml.jackson.annotation.JsonAlias;
import java.util.List;
import javax.annotation.Nullable;

public class HeuristicsFilter {

  private List<String> heuristics;
  @Nullable
  @JsonAlias("input-string")
  private String inputString;

  public List<String> getHeuristics() {
    return heuristics;
  }

  public void setHeuristics(List<String> heuristics) {
    this.heuristics = heuristics;
  }

  @Nullable
  public String getInputString() {
    return inputString;
  }

  public void setInputString(@Nullable String inputString) {
    this.inputString = inputString;
  }
}