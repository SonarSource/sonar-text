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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonar.plugins.secrets.api.task.RegexMatchingManager;
import org.sonar.plugins.secrets.configuration.model.matching.Matching;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;

class PatternMatcherTest {

  @RegisterExtension
  LogTesterJUnit5 logTester = new LogTesterJUnit5();

  @Test
  void testWithNoSuppliedPattern() {
    PatternMatcher noDetectionMatcher = PatternMatcher.build((Matching) null);
    List<CandidateMatch> matches = noDetectionMatcher.findMatches("test", "<test-rule-id>");
    assertThat(matches).isEmpty();
  }

  @Test
  void patternMatcherShouldNotRelyOnRegexDelimiters() {
    PatternMatcher patternMatcher = new PatternMatcher("pattern");
    List<CandidateMatch> matches = patternMatcher.findMatches("pattern pattern", "<test-rule-id>");
    assertThat(matches).hasSize(2);
  }

  @Test
  void patternMatcherShouldProduceTwoMatchesWithDelimiters() {
    PatternMatcher patternMatcher = new PatternMatcher("\\b(pattern)\\b");
    List<CandidateMatch> matches = patternMatcher.findMatches("pattern pattern", "<test-rule-id>");
    assertThat(matches).hasSize(2);
  }

  @Test
  void patternMatcherShouldTimeoutAndReturnNothingOnCatastrophicBacktracking() {
    RegexMatchingManager.setTimeoutMs(100);
    RegexMatchingManager.setUninterruptibleTimeoutMs(100);
    PatternMatcher patternMatcher = new PatternMatcher("(x+x+x+x+x+x+x+x+x+x+)+y");
    List<CandidateMatch> matches = patternMatcher.findMatches("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx", "<test-rule-id>");
    assertThat(matches).isEmpty();
    assertThat(logTester.logs())
      .containsExactly("Running pattern in rule with id \"<test-rule-id>\" on content of length 40 has timed out after 100ms. Related pattern is \"(x+x+x+x+x+x+x+x+x+x+)+y\".");
  }

  @Test
  void patternMatcherShouldTimeoutAndReturnSomeFirstResultOnCatastrophicBacktracking() {
    RegexMatchingManager.setTimeoutMs(100);
    RegexMatchingManager.setUninterruptibleTimeoutMs(100);
    PatternMatcher patternMatcher = new PatternMatcher("(\\d+|(x+x+x+x+x+x+x+x+x+x+)+y)");
    List<CandidateMatch> matches = patternMatcher.findMatches("1234xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx", "<test-rule-id>");
    assertThat(matches).hasSize(1);
    assertThat(logTester.logs()).containsExactly(
      "Running pattern in rule with id \"<test-rule-id>\" on content of length 44 has timed out after 100ms. Related pattern is \"(\\\\d+|(x+x+x+x+x+x+x+x+x+x+)+y)\".");
  }

  @Test
  void testCreationFromPattern() {
    var patternMatcher = PatternMatcher.build("pattern");

    var matches = patternMatcher.findMatches("line with pattern", "<test-rule-id>");

    assertThat(matches).hasSize(1);
  }

  @Test
  void shouldReturnMatchedNamedGroups() {
    var patternMatcher = PatternMatcher.build("(?<prefix>\\w+) (?<suffix>\\w+)");
    var content = "Match1 Match2";
    var ruleId = "<test-rule-id>";
    var namedGroups = List.of("prefix", "suffix");

    var matches = patternMatcher.findMatches(content, ruleId, namedGroups);

    assertThat(matches).hasSize(1);
    assertThat(matches.get(0).groups())
      .hasSize(2)
      .containsEntry("prefix", new CandidateMatch("Match1", 0, 6, emptyMap()))
      .containsEntry("suffix", new CandidateMatch("Match2", 7, 13, emptyMap()));
  }

  @Test
  void shouldNotReturnUnmatchedCaptureGroup() {
    var patternMatcher = PatternMatcher.build("(?<prefix>\\w+) (?<suffix>\\w+)?");
    var content = "Match1 ...";
    var ruleId = "<test-rule-id>";
    var namedGroups = List.of("prefix", "suffix");

    var matches = patternMatcher.findMatches(content, ruleId, namedGroups);

    assertThat(matches).hasSize(1);
    assertThat(matches.get(0).groups())
      .hasSize(1)
      .containsEntry("prefix", new CandidateMatch("Match1", 0, 6, emptyMap()))
      .doesNotContainKey("suffix");
  }

  @Test
  void shouldReturnFirstCaptureGroupAsMainLocation() {
    var patternMatcher = PatternMatcher.build("((?<prefix>\\w+)-\\d+)-(?<suffix>\\w+)");
    var content = "Match1-123-Match2";
    var ruleId = "<test-rule-id>";
    var namedGroups = List.of("prefix", "suffix");

    var matches = patternMatcher.findMatches(content, ruleId, namedGroups);

    assertThat(matches).hasSize(1);
    assertThat(matches.get(0).text())
      .describedAs("Should only include the first capturing group as main location")
      .isEqualTo("Match1-123");
  }
}
