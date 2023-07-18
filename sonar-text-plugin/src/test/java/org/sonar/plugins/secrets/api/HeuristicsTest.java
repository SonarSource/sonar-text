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

import java.util.Arrays;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class HeuristicsTest {
    @ParameterizedTest
    @CsvSource({
            "/home/user, true",
            "/home/username, false",
            "C:\\Users\\User, true",
            "asecretstr/ng, false"
    })
    void shouldDeterminePath(String input, boolean isPath) {
        Assertions.assertThat(Heuristics.isPath(input)).isEqualTo(isPath);
    }

    @ParameterizedTest
    @CsvSource({
            "https://sonarsource.com, true",
            "nonsense://secretstring, false",
    })
    void shouldDetermineUri(String input, boolean isUri) {
        Assertions.assertThat(Heuristics.isUri(input)).isEqualTo(isUri);
    }


    @ParameterizedTest
    @CsvSource({
            "path, https://sonarsource.com, false",
            "path;uri, https://sonarsource.com, true",
            "uri, https://sonarsource.com, true",
            "path, /home/user, true",
            "path;uri, /home/user, true",
            "uri, /home/user, false",
    })
    void shouldPerformChecksFromList(String heuristics, String input, boolean shouldMatch) {
        Assertions.assertThat(
                Heuristics.matchesHeuristics(input, Arrays.asList(heuristics.split(";")))
        ).isEqualTo(shouldMatch);
    }
}
