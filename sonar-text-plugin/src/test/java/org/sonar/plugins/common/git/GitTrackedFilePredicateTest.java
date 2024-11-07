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
package org.sonar.plugins.common.git;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.sonar.api.batch.fs.FilePredicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.plugins.common.TestUtils.inputFile;

class GitTrackedFilePredicateTest {

  // Workaround to get the base directory of the project
  private static final Path BASE_DIR = inputFile(Paths.get("")).path();

  @ParameterizedTest
  @MethodSource
  void shouldMatchSomeFilesWhenSomeUntracked(boolean isGitStatusSuccessful, Set<String> untrackedFiles, Map<String, Boolean> fileToExpectedMatch) {
    var gitService = mock(GitService.class);
    when(gitService.retrieveUntrackedFileNames(any())).thenReturn(new GitService.Result(isGitStatusSuccessful, untrackedFiles));
    FilePredicate predicate = new GitTrackedFilePredicate(BASE_DIR, gitService);

    for (Map.Entry<String, Boolean> entry : fileToExpectedMatch.entrySet()) {
      String file = entry.getKey();
      Boolean shouldMatch = entry.getValue();
      assertThat(predicate.apply(inputFile(Path.of(file)))).isEqualTo(shouldMatch);
    }
  }

  static Stream<Arguments> shouldMatchSomeFilesWhenSomeUntracked() {
    String fooJavaPath = Path.of("src", "foo.java").toString();
    String barJavaPath = Path.of("src", "bar.java").toString();
    return Stream.of(
      Arguments.of(true, Set.of(), Map.of("a.txt", true)),
      Arguments.of(true, Set.of("a.txt"), Map.of("a.txt", false, "b.txt", true)),
      Arguments.of(true, Set.of(fooJavaPath), Map.of(fooJavaPath, false, barJavaPath, true)),
      Arguments.of(false, Set.of(), Map.of("a.txt", true)),
      Arguments.of(false, Set.of("a.txt"), Map.of("a.txt", true)));
  }
}
