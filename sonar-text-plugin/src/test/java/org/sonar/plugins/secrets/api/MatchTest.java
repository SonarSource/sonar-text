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
}
