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
package org.sonar.plugins.secrets.configuration.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;

public enum Selectivity {
  @JsonEnumDefaultValue
  @JsonAlias("specific")
  SPECIFIC(0),
  @JsonAlias("providerGeneric")
  PROVIDER_GENERIC(1),
  // Lowest priority. If used as a selectivity, the rule will only raise on files without a language associated to it
  @JsonAlias("analyzerGeneric")
  ANALYZER_GENERIC(2);

  private final int priority;

  Selectivity(int priority) {
    this.priority = priority;
  }

  public int priority() {
    return priority;
  }
}
