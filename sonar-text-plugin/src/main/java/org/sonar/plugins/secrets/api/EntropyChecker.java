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
package org.sonar.plugins.secrets.api;

import java.util.HashMap;
import java.util.Map;

public final class EntropyChecker {

  public static final double DEFAULT_ENTROPY_THRESHOLD = 4.2;

  private EntropyChecker() {
    // utility class
  }

  public static boolean hasLowEntropy(String str) {
    return hasLowEntropy(str, DEFAULT_ENTROPY_THRESHOLD);
  }

  public static boolean hasLowEntropy(String str, double threshold) {
    return calculateShannonEntropy(str) < threshold;
  }

  public static double calculateShannonEntropy(String str) {
    if (str.isEmpty()) {
      return 0.0;
    }
    Map<Character, Integer> charMap = new HashMap<>();
    for (int i = 0; i < str.length(); i++) {
      char c = str.charAt(i);
      charMap.putIfAbsent(c, 0);
      charMap.put(c, charMap.get(c) + 1);
    }

    double entropy = 0.0;
    for (Integer count : charMap.values()) {
      double frequency = 1.0 * count / str.length();
      entropy -= frequency * Math.log(frequency) / Math.log(2);
    }
    return entropy;
  }

}
