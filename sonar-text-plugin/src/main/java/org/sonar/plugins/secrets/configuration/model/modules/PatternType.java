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
package org.sonar.plugins.secrets.configuration.model.modules;

public enum PatternType {
  PATTERN("pattern"),
  PATTERN_BEFORE("pattern-before"),
  PATTERN_AFTER("pattern-after"),
  PATTERN_AROUND("pattern-around"),
  PATTERN_NOT("pattern-not");

  private final String label;

  PatternType(String label) {
    this.label = label;
  }

  public static PatternType valueOfLabel(String label) {
    for (PatternType type : values()) {
      if (type.label.equals(label)) {
        return type;
      }
    }
    return null;
  }
}
