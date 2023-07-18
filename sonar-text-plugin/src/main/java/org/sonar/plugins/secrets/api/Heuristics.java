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
import java.util.regex.Pattern;

public class Heuristics {
    private Heuristics() {}

    private static final Pattern uriPattern = Pattern.compile("^(https?|ftps?|file|smtp|imap)://.*$");

    public static boolean matchesHeuristics(String candidateSecret, List<String> heuristics) {
        return heuristics.stream().anyMatch(h -> {
            switch (h) {
                case "path": return isPath(candidateSecret);
                case "uri": return isUri(candidateSecret);
                default: return false;
            }
        });
    }

    public static boolean isPath(String input) {
        long fileSeparatorCount = input.chars().filter(c -> c == '/' || c == '\\').count();
        return fileSeparatorCount >= 2 && fileSeparatorCount * 1. / input.length() > 0.15;
    }

    public static boolean isUri(String input) {
        return uriPattern.matcher(input).matches();
    }
}
