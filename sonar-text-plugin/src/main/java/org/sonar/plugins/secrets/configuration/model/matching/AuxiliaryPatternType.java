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
package org.sonar.plugins.secrets.configuration.model.matching;

public enum AuxiliaryPatternType {
  PATTERN_BEFORE("patternBefore"),
  PATTERN_AFTER("patternAfter"),
  PATTERN_AROUND("patternAround");

  private final String label;

  AuxiliaryPatternType(String label) {
    this.label = label;
  }

  public static AuxiliaryPatternType valueOfLabel(String label) {
    for (AuxiliaryPatternType type : values()) {
      if (type.label.equals(label)) {
        return type;
      }
    }
    return null;
  }
}
