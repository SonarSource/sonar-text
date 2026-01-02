/*
 * SonarQube Text Plugin
 * Copyright (C) 2021-2026 SonarSource Sàrl
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
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public abstract sealed class AbstractPostModule permits NamedPostModule,TopLevelPostModule {

  @Nullable
  private HeuristicsFilter heuristicFilter;
  private List<String> patternNot;
  @Nullable
  private StatisticalFilter statisticalFilter;
  private DecodedBase64Module decodedBase64Module;

  protected AbstractPostModule() {
  }

  protected AbstractPostModule(
    @Nullable DecodedBase64Module decodedBase64Module,
    @Nullable HeuristicsFilter heuristicFilter,
    @JsonSetter(nulls = Nulls.AS_EMPTY) List<String> patternNot,
    @Nullable StatisticalFilter statisticalFilter) {
    this.decodedBase64Module = decodedBase64Module;
    this.heuristicFilter = heuristicFilter;
    this.patternNot = patternNot;
    this.statisticalFilter = statisticalFilter;
  }

  @CheckForNull
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

  @CheckForNull
  public StatisticalFilter getStatisticalFilter() {
    return statisticalFilter;
  }

  public void setStatisticalFilter(@Nullable StatisticalFilter statisticalFilter) {
    this.statisticalFilter = statisticalFilter;
  }

  @CheckForNull
  public DecodedBase64Module getDecodedBase64Module() {
    return decodedBase64Module;
  }
}
