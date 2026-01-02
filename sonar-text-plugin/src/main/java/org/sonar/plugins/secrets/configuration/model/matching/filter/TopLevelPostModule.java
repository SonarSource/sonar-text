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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import java.util.List;
import javax.annotation.Nullable;

public final class TopLevelPostModule extends AbstractPostModule {
  private List<NamedPostModule> groups;

  public TopLevelPostModule() {
  }

  @JsonCreator
  public TopLevelPostModule(
    @JsonProperty("decodedBase64") @Nullable DecodedBase64Module decodedBase64Module,
    @JsonProperty("heuristicFilter") @Nullable HeuristicsFilter heuristicFilter,
    @JsonProperty("patternNot") @JsonSetter(nulls = Nulls.AS_EMPTY) List<String> patternNot,
    @JsonProperty("statisticalFilter") @Nullable StatisticalFilter statisticalFilter,
    @JsonProperty("groups") @JsonSetter(nulls = Nulls.AS_EMPTY) List<NamedPostModule> groups) {
    super(decodedBase64Module, heuristicFilter, patternNot, statisticalFilter);
    this.groups = groups;
  }

  public List<NamedPostModule> getGroups() {
    return groups;
  }
}
