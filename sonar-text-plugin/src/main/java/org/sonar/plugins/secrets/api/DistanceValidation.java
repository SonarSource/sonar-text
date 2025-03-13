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

/**
 * Utility class for validating distances between matches or offsets.
 */
public final class DistanceValidation {

  private DistanceValidation() {
    // Utility class
  }

  /**
   * Checks if the first match is before the second match.
   * @param firstMatch the first match
   * @param secondMatch the second match
   * @return true if the first match is before the second match, false otherwise
   */
  public static boolean isBefore(Match firstMatch, Match secondMatch) {
    // "fileEndOffset" is exclusive while "fileStartOffset" is inclusive, so we need to use <=
    return firstMatch.fileEndOffset() <= secondMatch.fileStartOffset();
  }

  /**
   * Checks if the first match is after the second match.
   * @param firstMatch the first match
   * @param secondMatch the second match
   * @return true if the first match is after the second match, false otherwise
   */
  public static boolean isAfter(Match firstMatch, Match secondMatch) {
    // "fileStartOffset" is inclusive while "fileEndOffset" is exclusive, so we need to use >=
    return firstMatch.fileStartOffset() >= secondMatch.fileEndOffset();
  }

  /**
   * Checks if the first match is in the specified distance of the second match.
   * @param firstMatch the first match
   * @param secondMatch the second match
   * @param distance the distance
   * @return true if the first match is in the specified distance of the second match, false otherwise
   */
  public static boolean inDistanceOf(Match firstMatch, Match secondMatch, int distance) {
    return inDistanceOf(
      firstMatch.fileStartOffset(), firstMatch.fileEndOffset(),
      secondMatch.fileStartOffset(), secondMatch.fileEndOffset(),
      distance);
  }

  /**
   * Checks if the first match is in the specified distance of the second match.
   * @param firstMatchStart the start offset of the first match
   * @param firstMatchEnd the end offset of the first match
   * @param secondMatchStart the start offset of the second match
   * @param secondMatchEnd the end offset of the second match
   * @param distance the distance
   * @return true if the first match is in the specified distance of the second match, false otherwise
   */
  public static boolean inDistanceOf(int firstMatchStart, int firstMatchEnd, int secondMatchStart, int secondMatchEnd, int distance) {
    int firstEndToSecondStartDistance = firstMatchEnd - secondMatchStart;
    int firstStartToSecondEndDistance = firstMatchStart - secondMatchEnd;
    boolean matchesOverlap = (firstEndToSecondStartDistance >= 0) && (firstStartToSecondEndDistance <= 0);

    if (matchesOverlap) {
      return true;
    } else {
      return Math.min(Math.abs(firstEndToSecondStartDistance), Math.abs(firstStartToSecondEndDistance)) <= distance;
    }
  }
}
