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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import java.util.List;
import javax.annotation.Nullable;

public abstract sealed class AbstractPostModule permits NamedPostModule,TopLevelPostModule {

  @Nullable
  private HeuristicsFilter heuristicFilter;
  private List<String> patternNot;
  @Nullable
  private StatisticalFilter statisticalFilter;

  protected AbstractPostModule() {
  }

  protected AbstractPostModule(
    @JsonProperty("heuristicFilter") @Nullable HeuristicsFilter heuristicFilter,
    @JsonProperty("patternNot") @JsonSetter(nulls = Nulls.AS_EMPTY) List<String> patternNot,
    @JsonProperty("statisticalFilter") @Nullable StatisticalFilter statisticalFilter) {
    this.heuristicFilter = heuristicFilter;
    this.patternNot = patternNot;
    this.statisticalFilter = statisticalFilter;
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
