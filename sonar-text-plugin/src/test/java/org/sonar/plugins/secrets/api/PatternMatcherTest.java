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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonar.plugins.secrets.api.task.RegexMatchingManager;
import org.sonar.plugins.secrets.configuration.model.matching.Matching;

import static org.assertj.core.api.Assertions.assertThat;

class PatternMatcherTest {

  @RegisterExtension
  LogTesterJUnit5 logTester = new LogTesterJUnit5();

  @Test
  void testWithNoSuppliedPattern() {
    PatternMatcher noDetectionMatcher = PatternMatcher.build((Matching) null);
    List<Match> matches = noDetectionMatcher.findIn("test", "<test-rule-id>");
    assertThat(matches).isEmpty();
  }

  @Test
  void patternMatcherShouldNotRelyOnRegexDelimiters() {
    PatternMatcher patternMatcher = new PatternMatcher("pattern");
    List<Match> matches = patternMatcher.findIn("pattern pattern", "<test-rule-id>");
    assertThat(matches).hasSize(2);
  }

  @Test
  void patternMatcherShouldProduceTwoMatchesWithDelimiters() {
    PatternMatcher patternMatcher = new PatternMatcher("\\b(pattern)\\b");
    List<Match> matches = patternMatcher.findIn("pattern pattern", "<test-rule-id>");
    assertThat(matches).hasSize(2);
  }

  @Test
  void patternMatcherShouldTimeoutAndReturnNothingOnCatastrophicBacktracking() {
    RegexMatchingManager.setTimeoutMs(100);
    RegexMatchingManager.setUninterruptibleTimeoutMs(100);
    PatternMatcher patternMatcher = new PatternMatcher("(x+x+x+x+x+x+x+x+x+x+)+y");
    List<Match> matches = patternMatcher.findIn("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx", "<test-rule-id>");
    assertThat(matches).isEmpty();
    assertThat(logTester.logs())
      .containsExactly("Running pattern in rule with id \"<test-rule-id>\" on content of length 40 has timed out after 100ms. Related pattern is \"(x+x+x+x+x+x+x+x+x+x+)+y\".");
  }

  @Test
  void patternMatcherShouldTimeoutAndReturnSomeFirstResultOnCatastrophicBacktracking() {
    RegexMatchingManager.setTimeoutMs(100);
    RegexMatchingManager.setUninterruptibleTimeoutMs(100);
    PatternMatcher patternMatcher = new PatternMatcher("(\\d+|(x+x+x+x+x+x+x+x+x+x+)+y)");
    List<Match> matches = patternMatcher.findIn("1234xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx", "<test-rule-id>");
    assertThat(matches).hasSize(1);
    assertThat(logTester.logs()).containsExactly(
      "Running pattern in rule with id \"<test-rule-id>\" on content of length 44 has timed out after 100ms. Related pattern is \"(\\\\d+|(x+x+x+x+x+x+x+x+x+x+)+y)\".");
  }

  @Test
  void testCreationFromPattern() {
    var patternMatcher = PatternMatcher.build("pattern");

    var matches = patternMatcher.findIn("line with pattern", "<test-rule-id>");

    assertThat(matches).hasSize(1);
  }
}
