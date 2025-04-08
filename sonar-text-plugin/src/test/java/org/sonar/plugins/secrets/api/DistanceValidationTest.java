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
package org.sonar.plugins.secrets.api;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.plugins.secrets.api.DistanceValidation.inDistanceOf;
import static org.sonar.plugins.secrets.api.DistanceValidation.isAfter;
import static org.sonar.plugins.secrets.api.DistanceValidation.isBefore;

class DistanceValidationTest {

  @Test
  void testFirstBeforeSecondMatch() {
    Match first = new Match("text", 10, 21, emptyMap());
    Match second = new Match("text", 21, 30, emptyMap());

    assertThat(isBefore(first, second)).isTrue();
    assertThat(isAfter(first, second)).isFalse();
    assertThat(isBefore(second, first)).isFalse();
    assertThat(isAfter(second, first)).isTrue();
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource
  void matchesShouldNotBeBeforeAndAfter(String testName, int secondMatchStartOffset, int secondMatchEndOffset) {
    Match first = new Match("text", 10, 20, emptyMap());
    Match second = new Match("text", secondMatchStartOffset, secondMatchEndOffset, emptyMap());

    assertThat(isBefore(first, second)).isFalse();
    assertThat(isAfter(first, second)).isFalse();
    assertThat(isBefore(second, first)).isFalse();
    assertThat(isAfter(second, first)).isFalse();
  }

  static Stream<Arguments> matchesShouldNotBeBeforeAndAfter() {
    return Stream.of(
      Arguments.of("Overlapping Matches", 15, 30),
      Arguments.of("Enclosed Match", 15, 17),
      Arguments.of("Sharing one character", 19, 25));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource
  void testInDistanceOfIsCalculatedCorrect(String testName, int secondMatchStartOffset, int secondMatchEndOffset, boolean isInRange) {
    Match first = new Match("text", 10, 20, emptyMap());
    Match second = new Match("text", secondMatchStartOffset, secondMatchEndOffset, emptyMap());

    int maxDistance = 3;
    assertThat(inDistanceOf(first, second, maxDistance)).isEqualTo(isInRange);
  }

  static Stream<Arguments> testInDistanceOfIsCalculatedCorrect() {
    return Stream.of(
      Arguments.of("Second Match after first out of range", 23, 30, true),
      Arguments.of("Second Match after first in range", 24, 30, false),
      Arguments.of("Second Match before first out of range", 0, 6, false),
      Arguments.of("Second Match before first in range", 0, 7, true),
      Arguments.of("Second Match wrapping first", 0, 30, true),
      Arguments.of("Second Match wrapping first", 9, 21, true),
      Arguments.of("Second Match overlapping first", 5, 15, true),
      Arguments.of("Second Match overlapping first", 9, 15, true),
      Arguments.of("Second Match overlapping first", 15, 21, true),
      Arguments.of("Second Match overlapping first", 15, 25, true),
      Arguments.of("First Match wrapping second", 15, 15, true),
      Arguments.of("First Match wrapping second", 11, 29, true));
  }
}
