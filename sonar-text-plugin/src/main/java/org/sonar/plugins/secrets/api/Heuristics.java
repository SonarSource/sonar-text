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

import java.util.List;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Heuristics {
  private Heuristics() {
  }

  private static final int MINIMAL_NUMBER_OF_SEPARATORS_IN_VALID_PATH = 2;
  private static final double MINIMAL_RATIO_OF_SEPARATOR_SYMBOLS_IN_VALID_PATH = 0.15;
  private static final Pattern uriPattern = Pattern.compile("^(https?|ftps?|file|smtp|imap)://.*$");

  private static final Logger LOG = LoggerFactory.getLogger(Heuristics.class);

  public static boolean matchesHeuristics(String candidateSecret, List<String> heuristics) {
    return heuristics.stream().anyMatch(heuristic -> {
      switch (heuristic) {
        case "path":
          return isPath(candidateSecret);
        case "uri":
          return isUri(candidateSecret);
        default:
          LOG.warn("Heuristic with the name `{}` is not supported", heuristic);
          return false;
      }
    });
  }

  public static boolean isPath(String input) {
    long fileSeparatorCount = input.chars().filter(c -> c == '/' || c == '\\').count();
    return fileSeparatorCount >= MINIMAL_NUMBER_OF_SEPARATORS_IN_VALID_PATH &&
      (fileSeparatorCount * 1. / input.length()) >= MINIMAL_RATIO_OF_SEPARATOR_SYMBOLS_IN_VALID_PATH;
  }

  public static boolean isUri(String input) {
    return uriPattern.matcher(input).matches();
  }
}
