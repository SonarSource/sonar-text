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
package org.sonar.plugins.secrets.configuration.model.matching.filter;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import java.util.List;
import javax.annotation.Nullable;

public record DecodedBase64Module(List<String> matchEach, List<String> matchNot, Alphabet alphabet) {
  public DecodedBase64Module(
    @JsonSetter(nulls = Nulls.AS_EMPTY) List<String> matchEach,
    @JsonSetter(nulls = Nulls.AS_EMPTY) List<String> matchNot,
    @Nullable Alphabet alphabet) {
    this.matchEach = matchEach;
    this.matchNot = matchNot;
    this.alphabet = alphabet == null ? Alphabet.DEFAULT : alphabet;
  }

  public enum Alphabet {
    @JsonProperty("default")
    DEFAULT,
    @JsonProperty("y64")
    Y64
  }
}
