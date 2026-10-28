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
package org.sonar.plugins.secrets.configuration.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;

public enum Selectivity {
  @JsonEnumDefaultValue
  @JsonAlias("specific")
  SPECIFIC(0),
  @JsonAlias("providerGeneric")
  PROVIDER_GENERIC(1),
  // Lowest priority. If used as a selectivity, the rule is suppressed only on files whose assigned language has an active
  // rule (outside the text and secrets repositories) with the same RSPEC key; see GenericSecretRuleCoverage.
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
