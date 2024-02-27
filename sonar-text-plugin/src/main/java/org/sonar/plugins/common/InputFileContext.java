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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextPointer;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.rule.RuleKey;

import static java.util.Objects.requireNonNull;

public class InputFileContext {

  private static final Logger LOG = LoggerFactory.getLogger(InputFileContext.class);
  private static final char LINE_FEED = '\n';

  private final SensorContext sensorContext;

  private final InputFile inputFile;

  private final boolean hasNonTextCharacters;

  private final List<String> lines;

  private final String normalizedContent;

  // Used to verify, that we don't raise more than one secret issue for any overlapping text range, regardless of the secret
  private final List<TextRange> reportedSecretIssues = new ArrayList<>();

  public InputFileContext(SensorContext sensorContext, InputFile inputFile) throws IOException {
    this.sensorContext = sensorContext;
    this.inputFile = inputFile;
    boolean checkNonTextCharacters = inputFile.language() == null;
    List<String> contentLines = new ArrayList<>();
    try (var in = inputFile.inputStream()) {
      var reader = new BufferedReader(new InputStreamReader(in, inputFile.charset()));
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

  public void reportTextIssue(RuleKey ruleKey, int line, String message) {
    var textRange = inputFile.selectLine(line);
    createAndSaveIssue(sensorContext, ruleKey, inputFile, textRange, message);
  }

  public void reportSecretIssue(RuleKey ruleKey, TextRange textRange, String message) {
    // Validation of overlapping textRange and adding to reportedSecretIssues does not create race-conditions, as we don't run checks in
    // parallel
    if (!overlappingSecretAlreadyReported(textRange)) {
      reportedSecretIssues.add(textRange);
      createAndSaveIssue(sensorContext, ruleKey, inputFile, textRange, message);
    } else {
      LOG.debug("Overlapping secret issue already reported {} on file {} and ruleKey {}", textRange, inputFile, ruleKey);
    }
  }

  private static synchronized void createAndSaveIssue(SensorContext sensorContext, RuleKey ruleKey, InputFile inputFile, TextRange textRange, String message) {
    // saving issues is not multi-thread safe in sonarAPI (all InputFileContext are using the same sensorContext)
    var issue = sensorContext.newIssue();
    issue
      .forRule(ruleKey)
      .at(issue.newLocation()
        .on(inputFile)
        .at(textRange)
        .message(message))
      .save();
  }

  public TextRange newTextRangeFromFileOffsets(int startOffset, int endOffset) {
    var lineNumber = 1;
    TextPointer startPointer = null;
    var currentLineStartOffset = 0;
    var charOffset = 0;
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

  public boolean overlappingSecretAlreadyReported(TextRange textRange) {
    return reportedSecretIssues.stream().anyMatch(textRange::overlap);
  }

  public InputFile getInputFile() {
    return inputFile;
  }

  public FileSystem getFileSystem() {
    return sensorContext.fileSystem();
  }
}
