/*
 * SonarQube Text Plugin
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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

/**
 * Post module applied to a value extracted inside another filter (e.g. the match extracted from a Base64-decoded
 * candidate). Unlike {@link NamedPostModule} it carries no {@code name}, and unlike {@link TopLevelPostModule} it has
 * no {@code groups}: it only holds the filter set reused by {@link org.sonar.plugins.secrets.api.filters.PostFilterFactory}.
 */
public final class NestedPostModule extends AbstractPostModule {

  public NestedPostModule() {
  }

  @JsonCreator
  public NestedPostModule(
    @JsonProperty("heuristicFilter") @Nullable HeuristicsFilter heuristicFilter,
    @JsonProperty("patternNot") @JsonSetter(nulls = Nulls.AS_EMPTY) List<String> patternNot,
    @JsonProperty("statisticalFilter") @Nullable StatisticalFilter statisticalFilter) {
    // Nested decodedBase64 is intentionally disallowed for now (matches the schema, avoids unbounded recursion).
    super(null, heuristicFilter, patternNot, statisticalFilter);
  }
}
