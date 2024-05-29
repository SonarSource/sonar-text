/*
 * SonarQube Text Plugin
 * Copyright (C) 2021-2024 SonarSource SA
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
package org.sonar.plugins.secrets.configuration.model.matching;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import javax.annotation.Nullable;
import org.sonar.plugins.secrets.configuration.deserialization.AuxiliaryPatternDeserializer;

@JsonDeserialize(using = AuxiliaryPatternDeserializer.class)
public class AuxiliaryPattern implements Match {

  private AuxiliaryPatternType type;
  private String pattern;
  @Nullable
  private Integer maxCharacterDistance;
  @Nullable
  private Integer maxLineDistance;

  public AuxiliaryPatternType getType() {
    return type;
  }

  public void setType(AuxiliaryPatternType type) {
    this.type = type;
  }

  public String getPattern() {
    return pattern;
  }

  public void setPattern(String pattern) {
    this.pattern = pattern;
  }

  @Nullable
  public Integer getMaxCharacterDistance() {
    return maxCharacterDistance;
  }

  public void setMaxCharacterDistance(@Nullable Integer maxCharacterDistance) {
    this.maxCharacterDistance = maxCharacterDistance;
  }

  @Nullable
  public Integer getMaxLineDistance() {
    return maxLineDistance;
  }

  public void setMaxLineDistance(@Nullable Integer maxLineDistance) {
    this.maxLineDistance = maxLineDistance;
  }
}
