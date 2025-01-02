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
