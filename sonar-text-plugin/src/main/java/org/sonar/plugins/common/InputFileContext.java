/*
 * SonarQube Text Plugin
 * Copyright (C) 2021-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.rule.RuleKey;

public class InputFileContext {

  private static final Logger LOG = LoggerFactory.getLogger(InputFileContext.class);
  private static final char LINE_FEED = '\n';

  private final SensorContext sensorContext;

  private final InputFile inputFile;

  private final boolean hasNonTextCharacters;

  private final List<String> lines;

  // sorted set of the start offset of each line
  private final TreeSet<Integer> lineStartOffsets;

  // maps the start offset of a line to the line number
  private final Map<Integer, Integer> offsetToLine;

  private final String normalizedContent;

  // Used to verify, that we don't raise more than one issue for any overlapping text range, regardless of the rule
  private final List<TextRange> reportedIssues = new ArrayList<>();

  public InputFileContext(SensorContext sensorContext, InputFile inputFile) throws IOException {
    this.sensorContext = sensorContext;
    this.inputFile = inputFile;
    this.lineStartOffsets = new TreeSet<>();
    this.offsetToLine = new HashMap<>();
    List<String> contentLines = new ArrayList<>();
    boolean checkNonTextCharacters = inputFile.language() == null;
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
    normalizedContent = buildNormalizedContentAndCalculateOffsets();
  }

  private String buildNormalizedContentAndCalculateOffsets() {
    var stringBuilder = new StringBuilder();
    for (var i = 0; i < lines.size(); i++) {
      int lineStartOffset = stringBuilder.length();
      int lineNumber = i + 1;
      lineStartOffsets.add(lineStartOffset);
      offsetToLine.put(lineStartOffset, lineNumber);

      stringBuilder.append(lines.get(i));
      if (i < lines.size() - 1) {
        stringBuilder.append(LINE_FEED);
      }
    }
    return stringBuilder.toString();
  }

  public void reportTextIssue(RuleKey ruleKey, int line, String message) {
    var textRange = inputFile.selectLine(line);
    createAndSaveIssue(sensorContext, ruleKey, inputFile, textRange, message);
  }

  public void reportIssueOnTextRange(RuleKey ruleKey, TextRange textRange, String message) {
    // Validation of overlapping textRange and adding to reportedIssues does not create race-conditions, as we don't run checks in
    // parallel
    if (!overlappingIssueAlreadyReported(textRange)) {
      reportedIssues.add(textRange);
      createAndSaveIssue(sensorContext, ruleKey, inputFile, textRange, message);
    } else {
      LOG.debug("Overlapping issue already reported {} on file {} and ruleKey {}", textRange, inputFile, ruleKey);
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
    var lineStartOfStartOffset = lineStartOffSet(startOffset);
    var lineStartOfEndOffset = lineStartOffSet(endOffset);

    var startOffSetLineNumber = offsetToLine.get(lineStartOfStartOffset);
    var endOffSetLineNumber = offsetToLine.get(lineStartOfEndOffset);

    try {
      var startPointer = inputFile.newPointer(startOffSetLineNumber, startOffset - lineStartOfStartOffset);
      var endPointer = inputFile.newPointer(endOffSetLineNumber, endOffset - lineStartOfEndOffset);
      return inputFile.newRange(startPointer, endPointer);
    } catch (IllegalArgumentException e) {
      throw new IllegalStateException("Invalid offsets: startOffset=" + startOffset + ", endOffset=" + endOffset, e);
    }
  }

  /**
   * Returns the start offset of the line containing the given offset
   * @param offset the offset
   * @return the start offset of the line containing the given offset
   * @throws IllegalStateException if the offset is invalid, most likely because it is negative
   */
  public int lineStartOffSet(int offset) {
    Integer floor = lineStartOffsets.floor(offset);
    if (floor == null) {
      throw new IllegalStateException("Invalid offset: offset=" + offset);
    }
    return floor;
  }

  /**
   * Returns the line number of the line containing the given offset
   * @param offset the offset
   * @return the line number of the line containing the given offset
   */
  public int offsetToLineNumber(int offset) {
    Integer floor = lineStartOffSet(offset);
    return offsetToLine.get(floor);
  }

  public boolean overlappingIssueAlreadyReported(TextRange textRange) {
    return reportedIssues.stream().anyMatch(textRange::overlap);
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

  public InputFile getInputFile() {
    return inputFile;
  }

  public FileSystem getFileSystem() {
    return sensorContext.fileSystem();
  }
}
