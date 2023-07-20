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
package org.sonar.plugins.secrets.api;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

class MatchTest {

  @Test
  void testFirstBeforeSecondMatch() {
    Match first = new Match("text", 10, 20);
    Match second = new Match("text", 21, 30);

    assertThat(first.isBefore(second)).isTrue();
    assertThat(first.isAfter(second)).isFalse();
    assertThat(second.isBefore(first)).isFalse();
    assertThat(second.isAfter(first)).isTrue();
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource
  void matchesShouldNotBeBeforeAndAfter(String testName, int secondMatchStartOffset, int secondMatchEndOffset) {
    Match first = new Match("text", 10, 20);
    Match second = new Match("text", secondMatchStartOffset, secondMatchEndOffset);

    assertThat(first.isBefore(second)).isFalse();
    assertThat(first.isAfter(second)).isFalse();
    assertThat(second.isBefore(first)).isFalse();
    assertThat(second.isAfter(first)).isFalse();
  }

  private static Stream<Arguments> matchesShouldNotBeBeforeAndAfter() {
    return Stream.of(
      Arguments.of("Overlapping Matches", 15, 30),
      Arguments.of("Enclosed Match", 15, 17),
      Arguments.of("Sharing one character", 20, 25));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource
  void testInRangeIsCalculatedCorrect(String testName, int secondMatchStartOffset, int secondMatchEndOffset, boolean isInRange) {
    Match first = new Match("text", 10, 20);
    Match second = new Match("text", secondMatchStartOffset, secondMatchEndOffset);

    int maxDistance = 3;
    assertThat(first.inDistanceOf(second, maxDistance)).isEqualTo(isInRange);
  }

  private static Stream<Arguments> testInRangeIsCalculatedCorrect() {
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
