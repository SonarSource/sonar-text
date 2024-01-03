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
