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
package org.sonar.plugins.common;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.StatusCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.NoWorkTreeException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.event.Level;
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.plugins.common.TestUtils.inputFile;

class GitTrackedFilePredicateTest {
  @RegisterExtension
  LogTesterJUnit5 logTester = new LogTesterJUnit5();

  @ParameterizedTest
  @MethodSource
  void shouldMatchSomeFilesWhenSomeUntracked(Set<String> untrackedFiles, Map<String, Boolean> fileToExpectedMatch) throws GitAPIException, IOException {
    var gitMock = setupGitMock(untrackedFiles);
    var gitWrapper = mock(GitSupplier.class);
    when(gitWrapper.getGit()).thenReturn(gitMock);
    FilePredicate predicate = new GitTrackedFilePredicate(gitWrapper);

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
      Arguments.of(Set.of(), Map.of("a.txt", true)),
      Arguments.of(Set.of("a.txt"), Map.of("a.txt", false, "b.txt", true)),
      Arguments.of(Set.of(fooJavaPath), Map.of(fooJavaPath, false, barJavaPath, true)));
  }

  @Test
  void shouldNotFilterWhenNoGit() throws IOException {
    logTester.setLevel(Level.DEBUG);

    var gitWrapper = mock(GitSupplier.class);
    when(gitWrapper.getGit()).thenThrow(NoWorkTreeException.class);
    FilePredicate predicate = new GitTrackedFilePredicate(gitWrapper);

    assertThat(predicate.apply(inputFile(Path.of("a.txt")))).isTrue();
    assertThat(predicate.apply(inputFile(Path.of("src", "b.txt")))).isTrue();
    assertThat(logTester.logs(Level.DEBUG).get(0)).contains("Unable to retrieve Git status, won't perform any exclusions");

    logTester.setLevel(Level.INFO);
  }

  static Git setupGitMock(Set<String> untrackedFiles) throws GitAPIException {
    var git = mock(Git.class);
    var statusCommandMock = mock(StatusCommand.class);
    when(git.status()).thenReturn(statusCommandMock);
    var statusMock = mock(Status.class);
    when(statusCommandMock.call()).thenReturn(statusMock);
    when(statusMock.getUntracked()).thenReturn(untrackedFiles);
    return git;
  }
}
