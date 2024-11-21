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
package org.sonar.plugins.secrets.configuration.model.matching.filter;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;

public class PostModule {

  @Nullable
  private HeuristicsFilter heuristicFilter;
  @JsonSetter(nulls = Nulls.SKIP)
  private List<String> patternNot = Collections.emptyList();
  @Nullable
  private StatisticalFilter statisticalFilter;

  public PostModule() {
    // Default constructor for deserializing with Jackson
  }

  @Nullable
  public HeuristicsFilter getHeuristicFilter() {
    return heuristicFilter;
  }

  public void setHeuristicFilter(@Nullable HeuristicsFilter heuristicFilter) {
    this.heuristicFilter = heuristicFilter;
  }

  public List<String> getPatternNot() {
    return patternNot;
  }

  public void setPatternNot(List<String> patternNot) {
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
