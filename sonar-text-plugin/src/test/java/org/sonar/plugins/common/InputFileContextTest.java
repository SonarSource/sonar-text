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
package org.sonar.plugins.common;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Collection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.batch.sensor.issue.Issue;
import org.sonar.api.rule.RuleKey;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.sonar.plugins.common.TestUtils.defaultSensorContext;
import static org.sonar.plugins.common.TestUtils.inputFile;

class InputFileContextTest {

  private SensorContextTester sensorContext;

  @BeforeEach
  void beforeEach() {
    sensorContext = defaultSensorContext();
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
    InputFile inputFile = inputFile(Path.of("invalid-path.txt"));
    assertThatThrownBy(() -> new InputFileContext(defaultSensorContext(), inputFile))
      .isInstanceOf(NoSuchFileException.class)
      .hasMessageContaining("invalid-path");
  }

  @Test
  void shouldFailToLoadContentIfCorruptedFile() throws IOException {
    InputFile inputFile = spy(inputFile("{}"));
    when(inputFile.inputStream()).thenThrow(new IOException("Fail to read file input stream"));
    assertThatThrownBy(() -> new InputFileContext(defaultSensorContext(), inputFile))
      .isInstanceOf(IOException.class)
      .hasMessageContaining("Fail to read file input stream");
  }

  @Test
  void shouldIdentifyBinaryFile() throws IOException {
    Path binaryFile = Path.of("build", "classes", "java", "test", "org", "sonar", "plugins", "common", "InputFileContextTest.class");
    InputFile inputFile = inputFile(binaryFile);
    InputFileContext ctx = new InputFileContext(defaultSensorContext(), inputFile);
    assertThat(ctx.hasNonTextCharacters()).isTrue();
    assertThat(ctx.lines()).isEmpty();
    assertThat(ctx.content()).isEmpty();
    String path = ctx.toString().replace('\\', '/');
    assertThat(path).isEqualTo("build/classes/java/test/org/sonar/plugins/common/InputFileContextTest.class");
  }

  @Test
  void shouldNotRaiseAnIssueOnOverlappingTextRange() throws IOException {
    InputFileContext ctx = inputFileContext("{some content inside this file}");
    TextRange range1 = ctx.newTextRangeFromFileOffsets(2, 6);

    ctx.reportSecretIssue(RuleKey.parse("s:42"), range1, "report secret issue 1");
    ctx.reportSecretIssue(RuleKey.parse("s:1337"), ctx.newTextRangeFromFileOffsets(0, 4), "overlapping secret");
    ctx.reportSecretIssue(RuleKey.parse("s:1337"), ctx.newTextRangeFromFileOffsets(4, 8), "overlapping secret");
    ctx.reportSecretIssue(RuleKey.parse("s:1337"), ctx.newTextRangeFromFileOffsets(3, 5), "overlapping secret");

    Collection<Issue> actual = sensorContext.allIssues();

    assertThat(actual)
      .hasSize(1)
      .anyMatch(issue -> issue.primaryLocation().message().equals("report secret issue 1"))
      .anyMatch(issue -> issue.primaryLocation().textRange().equals(range1));

  }

  private InputFileContext inputFileContext(String content) throws IOException {
    return new InputFileContext(sensorContext, inputFile(content));
  }

}
