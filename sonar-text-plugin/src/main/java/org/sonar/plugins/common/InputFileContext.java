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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextPointer;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.rule.RuleKey;

import static java.util.Objects.requireNonNull;

public class InputFileContext {

  private static final char LINE_FEED = '\n';

  private final SensorContext sensorContext;

  private final InputFile inputFile;

  private final boolean hasNonTextCharacters;

  private final List<String> lines;

  private final String normalizedContent;
  private final Set<String> raisedIssues = new HashSet<>();

  public InputFileContext(SensorContext sensorContext, InputFile inputFile) throws IOException {
    this.sensorContext = sensorContext;
    this.inputFile = inputFile;
    boolean checkNonTextCharacters = inputFile.language() == null;
    List<String> contentLines = new ArrayList<>();
    try (InputStream in = inputFile.inputStream()) {
      BufferedReader reader = new BufferedReader(new InputStreamReader(in, inputFile.charset()));
      String line = reader.readLine();
      while (line != null) {
        if (checkNonTextCharacters && BinaryFileUtils.hasNonTextCharacters(line)) {
          hasNonTextCharacters = true;
          lines = Collections.emptyList();
          normalizedContent = "";
          return;
        }
        contentLines.add(line);
        line = reader.readLine();
      }
    }
    hasNonTextCharacters = false;
    lines = contentLines;
    normalizedContent = String.join("\n", contentLines);
  }

  public boolean hasNonTextCharacters() {
    return hasNonTextCharacters;
  }

  public String content() {
    return normalizedContent;
  }

  public List<String> lines() {
    return lines;
  }

  @Override
  public String toString() {
    return inputFile.toString();
  }

  public void reportIssue(RuleKey ruleKey, int line, String message) {
    if (raisedIssues.add(ruleKey + ":" + line)) {
      NewIssue issue = sensorContext.newIssue();
      issue
        .forRule(ruleKey)
        .at(issue.newLocation()
          .on(inputFile)
          .at(inputFile.selectLine(line))
          .message(message))
        .save();
    }
  }

  public void reportIssue(RuleKey ruleKey, TextRange textRange, String message) {
    NewIssue issue = sensorContext.newIssue();
    issue
      .forRule(ruleKey)
      .at(issue.newLocation()
        .on(inputFile)
        .at(textRange)
        .message(message))
      .save();
  }

  public TextRange newTextRangeFromFileOffsets(int startOffset, int endOffset) {
    int lineNumber = 1;
    TextPointer startPointer = null;
    int currentLineStartOffset = 0;
    int charOffset = 0;
    for (; charOffset < normalizedContent.length(); charOffset++) {
      if (charOffset == startOffset) {
        startPointer = inputFile.newPointer(lineNumber, startOffset - currentLineStartOffset);
      }
      if (charOffset == endOffset) {
        TextPointer endPointer = inputFile.newPointer(lineNumber, endOffset - currentLineStartOffset);
        return inputFile.newRange(requireNonNull(startPointer), endPointer);
      }

      if (normalizedContent.charAt(charOffset) == LINE_FEED) {
        lineNumber++;
        currentLineStartOffset = charOffset + 1;
      }
    }
    // check again in case the end offset is after the last character
    if (startPointer != null && charOffset == endOffset) {
      TextPointer endPointer = inputFile.newPointer(lineNumber, endOffset - currentLineStartOffset);
      return inputFile.newRange(requireNonNull(startPointer), endPointer);
    }
    // should not happen as input parameters are supposed to be valid offsets
    throw new IllegalStateException("Invalid offsets: startOffset=" + startOffset + ", endOffset=" + endOffset);
  }

  public InputFile getInputFile() {
    return inputFile;
  }
}
