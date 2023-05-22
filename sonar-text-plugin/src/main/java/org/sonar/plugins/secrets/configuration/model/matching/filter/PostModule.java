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
import javax.annotation.Nullable;

public class PostModule {

  @Nullable
  @JsonAlias("heuristic-filter")
  private HeuristicsFilter heuristicFilter;
  @Nullable
  @JsonAlias("pattern-not")
  private String patternNot;
  @Nullable
  @JsonAlias("statistical-filter")
  private StatisticalFilter statisticalFilter;

  @Nullable
  public HeuristicsFilter getHeuristicFilter() {
    return heuristicFilter;
  }

  public void setHeuristicFilter(@Nullable HeuristicsFilter heuristicFilter) {
    this.heuristicFilter = heuristicFilter;
  }

  @Nullable
  public String getPatternNot() {
    return patternNot;
  }

  public void setPatternNot(@Nullable String patternNot) {
    this.patternNot = patternNot;
  }

  @Nullable
  public StatisticalFilter getStatisticalFilter() {
    return statisticalFilter;
  }

  public void setStatisticalFilter(@Nullable StatisticalFilter statisticalFilter) {
    this.statisticalFilter = statisticalFilter;
  }
}
