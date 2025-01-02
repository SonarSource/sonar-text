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
package org.sonar.plugins.secrets.configuration.model.matching;

public enum BooleanCombinationType {
  MATCH_EITHER("matchEither"),
  MATCH_EACH("matchEach"),
  MATCH_NOT("matchNot");

  private final String label;

  BooleanCombinationType(String label) {
    this.label = label;
  }

  public static BooleanCombinationType valueOfLabel(String label) {
    for (BooleanCombinationType type : values()) {
      if (type.label.equals(label)) {
        return type;
      }
    }
    return null;
  }
}
