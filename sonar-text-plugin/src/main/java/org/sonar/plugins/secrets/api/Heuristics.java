/*
 * SonarQube Text Plugin
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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
    return heuristics.stream().anyMatch(heuristic -> matchesHeuristic(candidateSecret, heuristic));
  }

  public static boolean matchesHeuristic(String candidateSecret, String heuristic) {
    return switch (heuristic) {
      case "path" -> isPath(candidateSecret);
      case "uri" -> isUri(candidateSecret);
      default -> {
        LOG.warn("Heuristic with the name `{}` is not supported", heuristic);
        yield false;
      }
    };
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
