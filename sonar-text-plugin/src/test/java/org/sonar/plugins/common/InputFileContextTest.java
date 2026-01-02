/*
 * SonarQube Text Plugin
 * Copyright (C) 2021-2026 SonarSource Sàrl
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
package org.sonar.plugins.common;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.event.Level;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.batch.sensor.issue.IssueLocation;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonar.plugins.secrets.configuration.model.Selectivity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.from;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.sonar.plugins.common.TestUtils.inputFile;
import static org.sonar.plugins.common.TestUtils.inputFileFromPath;

class InputFileContextTest {

  private SensorContextTester sensorContext;
  @RegisterExtension
  LogTesterJUnit5 logTester = new LogTesterJUnit5();

  @BeforeEach
  void beforeEach() {
    sensorContext = new TestUtils().defaultSensorContext();
  }

  @Test
  void shouldConvertWhenOffsetAreIdenticalAndOnSameLine() throws IOException {
    InputFileContext ctx = inputFileContext("first");

    TextRange range = ctx.newTextRangeFromFileOffsets(0, 1);

    assertThat(range.start().line()).isOne();
    assertThat(range.start().lineOffset()).isZero();
    assertThat(range.end().line()).isOne();
    assertThat(range.end().lineOffset()).isOne();
  }

  @Test
  void shouldConvertWhenOffsetsAreOnDifferentLinesSeparatedByLineFeed() throws IOException {
    InputFileContext ctx = inputFileContext("first\nsecond");

    TextRange range = ctx.newTextRangeFromFileOffsets(0, 12);

    assertThat(range.start().line()).isOne();
    assertThat(range.start().lineOffset()).isZero();
    assertThat(range.end().line()).isEqualTo(2);
    assertThat(range.end().lineOffset()).isEqualTo(6);
  }

  @Test
  void shouldConvertWhenOffsetsAreNotOnTheFirstLine() throws IOException {
    InputFileContext ctx = inputFileContext("first\nsecond\nthird\nfourth");

    TextRange range = ctx.newTextRangeFromFileOffsets(19, 25);

    assertThat(range.start().line()).isEqualTo(4);
    assertThat(range.start().lineOffset()).isZero();
    assertThat(range.end().line()).isEqualTo(4);
    assertThat(range.end().lineOffset()).isEqualTo(6);
  }

  @ParameterizedTest
  @ValueSource(strings = {
    "\n",
    "\r\n",
    "\r",
  })
  void lineSeparatorsShouldBeNormalized(String separator) throws IOException {
    InputFileContext ctx = inputFileContext("first" + separator + "second");

    TextRange range = ctx.newTextRangeFromFileOffsets(0, 12);

    assertThat(ctx.lines()).hasSize(2);
    assertThat(range.start().line()).isEqualTo(1);
    assertThat(range.start().lineOffset()).isZero();
    assertThat(range.end().line()).isEqualTo(2);
    assertThat(range.end().lineOffset()).isEqualTo(6);
  }

  @Test
  void shouldFailWhenOffsetsInvalid() throws IOException {
    InputFileContext ctx = inputFileContext("01234");

    assertThatThrownBy(() -> ctx.newTextRangeFromFileOffsets(5, 6))
      .hasMessage("Invalid offsets: startOffset=5, endOffset=6");

    assertThatThrownBy(() -> ctx.newTextRangeFromFileOffsets(6, 5))
      .hasMessage("Invalid offsets: startOffset=6, endOffset=5");

    assertThatThrownBy(() -> ctx.newTextRangeFromFileOffsets(2, 6))
      .hasMessage("Invalid offsets: startOffset=2, endOffset=6");
  }

  @Test
  void shouldFailToLoadContentIfFileDoesNotExist() {
    InputFile inputFile = inputFileFromPath(Path.of("invalid-path.txt"));
    assertThatThrownBy(() -> new InputFileContext(sensorContext, inputFile))
      .isInstanceOf(NoSuchFileException.class)
      .hasMessageContaining("invalid-path");
  }

  @Test
  void shouldFailToLoadContentIfCorruptedFile() throws IOException {
    InputFile inputFile = spy(inputFile("{}"));
    when(inputFile.inputStream()).thenThrow(new IOException("Fail to read file input stream"));
    assertThatThrownBy(() -> new InputFileContext(sensorContext, inputFile))
      .isInstanceOf(IOException.class)
      .hasMessageContaining("Fail to read file input stream");
  }

  @Test
  void shouldIdentifyBinaryFile() throws IOException {
    Path binaryFile = Path.of("build", "classes", "java", "test", "org", "sonar", "plugins", "common", "InputFileContextTest.class");
    InputFile inputFile = inputFileFromPath(binaryFile);
    InputFileContext ctx = new InputFileContext(sensorContext, inputFile);
    assertThat(ctx.hasNonTextCharacters()).isTrue();
    assertThat(ctx.lines()).isEmpty();
    assertThat(ctx.content()).isEmpty();
    String path = ctx.toString().replace('\\', '/');
    assertThat(path).isEqualTo("build/classes/java/test/org/sonar/plugins/common/InputFileContextTest.class");
  }

  @ParameterizedTest
  @ValueSource(strings = {"ANALYZER_GENERIC", "PROVIDER_GENERIC", "SPECIFIC"})
  void shouldRaiseExactlyOneIssueOnOverlappingTextRange(Selectivity selectivity) throws IOException {
    var ctx = inputFileContext("{some content inside this file}");
    var range1 = ctx.newTextRangeFromFileOffsets(2, 6);

    ctx.reportIssueOnTextRange(RuleKey.parse("s:42"), selectivity, range1, "report secret issue 1");
    ctx.reportIssueOnTextRange(RuleKey.parse("s:1337"), selectivity, ctx.newTextRangeFromFileOffsets(0, 4), "overlapping secret 1");
    ctx.reportIssueOnTextRange(RuleKey.parse("s:1337"), selectivity, ctx.newTextRangeFromFileOffsets(4, 8), "overlapping secret 2");
    ctx.reportIssueOnTextRange(RuleKey.parse("s:1337"), selectivity, ctx.newTextRangeFromFileOffsets(3, 5), "overlapping secret 3");
    ctx.flushIssues();

    var actual = sensorContext.allIssues();

    assertThat(actual)
      .hasSize(1)
      .anySatisfy(issue -> assertThat(issue.primaryLocation()).returns("report secret issue 1", from(IssueLocation::message)))
      .anySatisfy(issue -> assertThat(issue.primaryLocation().textRange()).isEqualTo(range1));
  }

  @Test
  void shouldRetainSingleIssueWhenNoOverlaps() throws IOException {
    var ctx = inputFileContext("content with no overlaps");
    var range1 = ctx.newTextRangeFromFileOffsets(0, 5);
    var range2 = ctx.newTextRangeFromFileOffsets(6, 10);

    ctx.reportIssueOnTextRange(RuleKey.parse("s:1"), Selectivity.PROVIDER_GENERIC, range1, "Issue 1");
    ctx.reportIssueOnTextRange(RuleKey.parse("s:2"), Selectivity.PROVIDER_GENERIC, range2, "Issue 2");
    ctx.flushIssues();

    var actual = sensorContext.allIssues();

    assertThat(actual)
      .hasSize(2)
      .anySatisfy(issue -> assertThat(issue.primaryLocation()).returns("Issue 1", from(IssueLocation::message)))
      .anySatisfy(issue -> assertThat(issue.primaryLocation()).returns("Issue 2", from(IssueLocation::message)));
  }

  @ParameterizedTest
  @MethodSource
  void shouldRetainLowestSelectivityIssueWhenOverlapping(Selectivity selectivityIssue1, Selectivity selectivityIssue2, Selectivity selectivityRaisedIssue) throws IOException {
    var ctx = inputFileContext("overlapping content");
    var range1 = ctx.newTextRangeFromFileOffsets(0, 5);
    var range2 = ctx.newTextRangeFromFileOffsets(3, 8);

    ctx.reportIssueOnTextRange(RuleKey.parse("s:1"), selectivityIssue1, range1, selectivityIssue1.toString());
    ctx.reportIssueOnTextRange(RuleKey.parse("s:2"), selectivityIssue2, range2, selectivityIssue2.toString());
    ctx.flushIssues();

    var actual = sensorContext.allIssues();

    assertThat(actual)
      .hasSize(1)
      .anySatisfy(issue -> assertThat(issue.primaryLocation()).returns(selectivityRaisedIssue.toString(), from(IssueLocation::message)));
  }

  static Stream<Arguments> shouldRetainLowestSelectivityIssueWhenOverlapping() {
    return Stream.of(
      Arguments.of(Selectivity.SPECIFIC, Selectivity.PROVIDER_GENERIC, Selectivity.SPECIFIC),
      Arguments.of(Selectivity.SPECIFIC, Selectivity.ANALYZER_GENERIC, Selectivity.SPECIFIC),
      Arguments.of(Selectivity.PROVIDER_GENERIC, Selectivity.ANALYZER_GENERIC, Selectivity.PROVIDER_GENERIC));
  }

  @Test
  void shouldHandleAllSelectivities() throws IOException {
    var ctx = inputFileContext("all selectivities");
    var range1 = ctx.newTextRangeFromFileOffsets(0, 5);
    var range2 = ctx.newTextRangeFromFileOffsets(3, 8);
    var range3 = ctx.newTextRangeFromFileOffsets(1, 7);

    ctx.reportIssueOnTextRange(RuleKey.parse("s:1"), Selectivity.ANALYZER_GENERIC, range1, Selectivity.ANALYZER_GENERIC.toString());
    ctx.reportIssueOnTextRange(RuleKey.parse("s:2"), Selectivity.PROVIDER_GENERIC, range2, Selectivity.PROVIDER_GENERIC.toString());
    ctx.reportIssueOnTextRange(RuleKey.parse("s:3"), Selectivity.SPECIFIC, range3, Selectivity.SPECIFIC.toString());
    ctx.flushIssues();

    var actual = sensorContext.allIssues();

    assertThat(actual)
      .hasSize(1)
      .anySatisfy(issue -> assertThat(issue.primaryLocation()).returns(Selectivity.SPECIFIC.toString(), from(IssueLocation::message)));
  }

  @Test
  void shouldHandleMultipleOverlappingBuckets() throws IOException {
    var ctx = inputFileContext("multiple overlapping ranges");
    var range1 = ctx.newTextRangeFromFileOffsets(0, 5);
    var range2 = ctx.newTextRangeFromFileOffsets(3, 8);
    var range3 = ctx.newTextRangeFromFileOffsets(10, 15);
    var range4 = ctx.newTextRangeFromFileOffsets(12, 18);

    ctx.reportIssueOnTextRange(RuleKey.parse("s:1"), Selectivity.PROVIDER_GENERIC, range1, "Generic Issue 1");
    ctx.reportIssueOnTextRange(RuleKey.parse("s:2"), Selectivity.SPECIFIC, range2, "Specific Issue 1");
    ctx.reportIssueOnTextRange(RuleKey.parse("s:3"), Selectivity.ANALYZER_GENERIC, range3, "Language Generic Issue 2");
    ctx.reportIssueOnTextRange(RuleKey.parse("s:4"), Selectivity.PROVIDER_GENERIC, range4, "Provider Generic Issue 2");
    ctx.flushIssues();

    var actual = sensorContext.allIssues();

    assertThat(actual)
      .hasSize(2)
      .anySatisfy(issue -> assertThat(issue.primaryLocation()).returns("Specific Issue 1", from(IssueLocation::message)))
      .anySatisfy(issue -> assertThat(issue.primaryLocation()).returns("Provider Generic Issue 2", from(IssueLocation::message)));
  }

  @Test
  void shouldLogOverlappingIssuesWhenDebugEnabled() throws IOException {
    logTester.setLevel(Level.DEBUG);

    var ctx = inputFileContext("debug overlapping issues");
    var range1 = ctx.newTextRangeFromFileOffsets(0, 5);
    var range2 = ctx.newTextRangeFromFileOffsets(3, 8);

    ctx.reportIssueOnTextRange(RuleKey.parse("s:1"), Selectivity.PROVIDER_GENERIC, range1, "Generic Issue");
    ctx.reportIssueOnTextRange(RuleKey.parse("s:2"), Selectivity.SPECIFIC, range2, "Strict Issue");

    ctx.flushIssues();

    assertThat(logTester.getLogs(Level.DEBUG))
      .anyMatch(it -> it.getFormattedMsg().contains(
        "Overlapping issues detected for file file.txt: Issue [s:2, Range[from [line=1, lineOffset=3] to [line=1, lineOffset=8]]] " +
          "prioritized over [s:1, Range[from [line=1, lineOffset=0] to [line=1, lineOffset=5]]]"));
  }

  @Test
  void shouldHandleEmptyReportedIssues() throws IOException {
    var ctx = inputFileContext("no issues reported");
    ctx.flushIssues();

    var actual = sensorContext.allIssues();

    assertThat(actual).isEmpty();
  }

  private InputFileContext inputFileContext(String content) throws IOException {
    return new InputFileContext(sensorContext, inputFile(content));
  }

}
