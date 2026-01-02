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
package org.sonar.plugins.secrets.api;

import java.util.Arrays;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class HeuristicsTest {
  @ParameterizedTest
  @CsvSource({
    "/home/user, true",
    "/home/username, false",
    "C:\\Users\\User, true",
    "asecretstr/ng, false"
  })
  void shouldDeterminePath(String input, boolean isPath) {
    assertThat(Heuristics.isPath(input)).isEqualTo(isPath);
  }

  @ParameterizedTest
  @CsvSource({
    "https://sonarsource.com, true",
    "nonsense://secretstring, false",
  })
  void shouldDetermineUri(String input, boolean isUri) {
    assertThat(Heuristics.isUri(input)).isEqualTo(isUri);
  }

  @ParameterizedTest
  @CsvSource({
    "path, https://sonarsource.com, false",
    "path;uri, https://sonarsource.com, true",
    "uri, https://sonarsource.com, true",
    "path, /home/user, true",
    "path;uri, /home/user, true",
    "uri, /home/user, false",
    "unknown, /home/user, false",
    "'', /home/user, false",
  })
  void shouldPerformChecksFromList(String heuristics, String input, boolean shouldMatch) {
    assertThat(
      Heuristics.matchesHeuristics(input, Arrays.asList(heuristics.split(";")))).isEqualTo(shouldMatch);
  }
}
