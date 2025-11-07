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
import org.sonar.plugins.common.TextAndSecretsSensor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.plugins.common.TestUtils.inputFileFromPath;

class GitTrackedFilePredicateTest {

  // Workaround to get the base directory of the project
  private static final Path BASE_DIR = inputFileFromPath(Paths.get("")).path();

  @ParameterizedTest
  @MethodSource
  void shouldMatchSomeFilesWhenSomeUntracked(boolean isGitStatusSuccessful, Set<String> untrackedFiles, Map<String, Boolean> fileToExpectedMatch) {
    var gitService = mock(GitService.class);
    when(gitService.retrieveUntrackedFileNames()).thenReturn(new GitService.UntrackedFileNamesResult(isGitStatusSuccessful, untrackedFiles));
    FilePredicate predicate = new GitTrackedFilePredicate(BASE_DIR, gitService, TextAndSecretsSensor.LANGUAGE_FILE_PREDICATE);

    for (Map.Entry<String, Boolean> entry : fileToExpectedMatch.entrySet()) {
      String file = entry.getKey();
      Boolean shouldMatch = entry.getValue();
      assertThat(predicate.apply(inputFileFromPath(Path.of(file)))).isEqualTo(shouldMatch);
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
