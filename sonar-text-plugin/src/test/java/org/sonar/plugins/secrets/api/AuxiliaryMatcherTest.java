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

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.sonar.plugins.secrets.configuration.model.matching.AuxiliaryPatternType;

import static org.assertj.core.api.Assertions.assertThat;

class AuxiliaryMatcherTest {

  @ParameterizedTest
  @MethodSource
  void auxiliaryPatternShouldBeDetectedAndCandidateSecretShouldNotBeRemoved(AuxiliaryPatternType patternType, String content,
    String auxiliaryPattern) {
    AuxiliaryMatcher auxiliaryMatcher = new AuxiliaryMatcher(
      patternType, new PatternMatcher("\\b(" + auxiliaryPattern + ")\\b"));

    PatternMatcher patternMatcher = new PatternMatcher("\\b(candidate secret)\\b");
    List<Match> candidateSecrets = patternMatcher.findIn(content);

    List<Match> result = auxiliaryMatcher.filter(candidateSecrets, content);

    assertThat(result).containsExactlyElementsOf(candidateSecrets);
  }

  private static Stream<Arguments> auxiliaryPatternShouldBeDetectedAndCandidateSecretShouldNotBeRemoved() {
    return Stream.of(
      Arguments.of(AuxiliaryPatternType.PATTERN_BEFORE, "auxiliaryPattern and candidate secret", "auxiliaryPattern"),
      Arguments.of(AuxiliaryPatternType.PATTERN_BEFORE, "auxiliaryPattern and candidate secret and auxiliaryPattern", "auxiliaryPattern"),
      Arguments.of(AuxiliaryPatternType.PATTERN_BEFORE, "auxiliaryPattern, auxiliaryPattern and candidate secret", "auxiliaryPattern"),
      Arguments.of(AuxiliaryPatternType.PATTERN_AFTER, "candidate secret and auxiliaryPattern", "auxiliaryPattern"),
      Arguments.of(AuxiliaryPatternType.PATTERN_AFTER, "auxiliaryPattern and candidate secret and auxiliaryPattern", "auxiliaryPattern"),
      Arguments.of(AuxiliaryPatternType.PATTERN_AFTER, "candidate secret and auxiliaryPattern, auxiliaryPattern", "auxiliaryPattern"),
      Arguments.of(AuxiliaryPatternType.PATTERN_AROUND, "auxiliaryPattern and candidate secret and auxiliaryPattern", "auxiliaryPattern"),
      Arguments.of(AuxiliaryPatternType.PATTERN_AROUND, "auxiliaryPattern, auxiliaryPattern and candidate secret and auxiliaryPattern, " +
        "auxiliaryPattern", "auxiliaryPattern"),
      Arguments.of(AuxiliaryPatternType.PATTERN_AROUND, "auxiliaryPattern and candidate secret", "auxiliaryPattern"),
      Arguments.of(AuxiliaryPatternType.PATTERN_AROUND, "auxiliaryPattern, auxiliaryPattern and candidate secret", "auxiliaryPattern"),
      Arguments.of(AuxiliaryPatternType.PATTERN_AROUND, "candidate secret and auxiliaryPattern", "auxiliaryPattern"),
      Arguments.of(AuxiliaryPatternType.PATTERN_AROUND, "candidate secret and auxiliaryPattern, auxiliaryPattern", "auxiliaryPattern"),

      //PATTERN_NOT is not supported so we don't expect differences
      Arguments.of(AuxiliaryPatternType.PATTERN_NOT, "candidate secret and auxiliaryPattern", "auxiliaryPattern"),
      Arguments.of(AuxiliaryPatternType.PATTERN_NOT, "candidate secret", "auxiliaryPattern")
    );
  }

  @ParameterizedTest
  @MethodSource
  void auxiliaryPatternShouldRemoveCandidateSecrets(AuxiliaryPatternType patternType, String content, String auxiliaryPattern) {
    AuxiliaryMatcher auxiliaryMatcher = new AuxiliaryMatcher(
      patternType, new PatternMatcher("\\b(" + auxiliaryPattern + ")\\b"));

    PatternMatcher patternMatcher = new PatternMatcher("\\b(candidate secret)\\b");
    List<Match> candidateSecrets = patternMatcher.findIn(content);

    List<Match> result = auxiliaryMatcher.filter(candidateSecrets, content);

    assertThat(result).isEmpty();
  }

  private static Stream<Arguments> auxiliaryPatternShouldRemoveCandidateSecrets() {
    return Stream.of(
      Arguments.of(AuxiliaryPatternType.PATTERN_BEFORE, "otherWord and candidate secret", "auxiliaryPattern"),
      Arguments.of(AuxiliaryPatternType.PATTERN_BEFORE, "word and candidate secret", "didat"),
      Arguments.of(AuxiliaryPatternType.PATTERN_BEFORE, "word and candidate secret and auxiliaryPattern", "auxiliaryPattern"),
      Arguments.of(AuxiliaryPatternType.PATTERN_AFTER, "candidate secret and other word", "auxiliaryPattern"),
      Arguments.of(AuxiliaryPatternType.PATTERN_AFTER, "word and candidate secret", "didat"),
      Arguments.of(AuxiliaryPatternType.PATTERN_AFTER, "auxiliaryPattern and candidate secret and other word", "auxiliaryPattern"),
      Arguments.of(AuxiliaryPatternType.PATTERN_AROUND, "something else and candidate secret and other word", "auxiliaryPattern"),
      Arguments.of(AuxiliaryPatternType.PATTERN_AROUND, "word and candidate secret", "didat")
    );
  }
}
