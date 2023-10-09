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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonar.plugins.secrets.api.task.ExecutorServiceManager;
import org.sonar.plugins.secrets.configuration.model.matching.Matching;

import static org.assertj.core.api.Assertions.assertThat;

class PatternMatcherTest {

  @RegisterExtension
  LogTesterJUnit5 logTester = new LogTesterJUnit5();

  @Test
  void testWithNoSuppliedPattern() {
    PatternMatcher noDetectionMatcher = PatternMatcher.build((Matching) null);
    List<Match> matches = noDetectionMatcher.findIn("test");
    assertThat(matches).isEmpty();
  }

  @Test
  void patternMatcherShouldNotRelyOnRegexDelimiters() {
    PatternMatcher patternMatcher = new PatternMatcher("pattern");
    List<Match> matches = patternMatcher.findIn("pattern pattern");
    assertThat(matches).hasSize(2);
  }

  @Test
  void patternMatcherShouldProduceTwoMatchesWithDelimiters() {
    PatternMatcher patternMatcher = new PatternMatcher("\\b(pattern)\\b");
    List<Match> matches = patternMatcher.findIn("pattern pattern");
    assertThat(matches).hasSize(2);
  }

  @Test
  void patternMatcherShouldTimeoutAndReturnNothingOnCatastrophicBacktracking() {
    ExecutorServiceManager.timeoutMs = 100;
    ExecutorServiceManager.uninterruptibleTimeoutMs = 100;
    PatternMatcher patternMatcher = new PatternMatcher("(x+x+x+x+x+x+x+x+x+x+)+y");
    List<Match> matches = patternMatcher.findIn("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
    assertThat(matches).isEmpty();
    assertThat(logTester.logs()).containsExactly("Running pattern '(x+x+x+x+x+x+x+x+x+x+)+y' on content(40) has timed out (100ms)");
  }

  @Test
  void patternMatcherShouldTimeoutAndReturnSomeFirstResultOnCatastrophicBacktracking() {
    ExecutorServiceManager.timeoutMs = 100;
    ExecutorServiceManager.uninterruptibleTimeoutMs = 100;
    PatternMatcher patternMatcher = new PatternMatcher("(\\d+|(x+x+x+x+x+x+x+x+x+x+)+y)");
    List<Match> matches = patternMatcher.findIn("1234xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
    assertThat(matches).hasSize(1);
    assertThat(logTester.logs()).containsExactly("Running pattern '(\\d+|(x+x+x+x+x+x+x+x+x+x+)+y)' on content(44) has timed out (100ms)");
  }
}
