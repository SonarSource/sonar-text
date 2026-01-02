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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.TreeSet;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.rule.RuleKey;
import org.sonar.plugins.secrets.configuration.model.Selectivity;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.joining;

public class InputFileContext {

  private static final Logger LOG = LoggerFactory.getLogger(InputFileContext.class);
  private static final char LINE_FEED = '\n';

  private final SensorContext sensorContext;

  private final InputFile inputFile;

  private final boolean hasNonTextCharacters;

  private final List<String> lines;

  // sorted set of the start offset of each line
  private final NavigableSet<Integer> lineStartOffsets = new TreeSet<>();

  // maps the start offset of a line to the line number
  private final Map<Integer, Integer> offsetToLine = new HashMap<>();

  private final String normalizedContent;

  // list of issues that were detected and should be reported after filtering out overlaps
  private final List<CandidateIssue> candidateIssues = new ArrayList<>();

  public InputFileContext(SensorContext sensorContext, InputFile inputFile) throws IOException {
    this.sensorContext = sensorContext;
    this.inputFile = inputFile;
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

  public void reportIssueOnTextRange(RuleKey ruleKey, Selectivity ruleSelectivity, TextRange textRange, String message) {
    candidateIssues.add(new CandidateIssue(ruleKey, ruleSelectivity, textRange, message));
  }

  public void reportIssueOnFile(RuleKey ruleKey, String message) {
    createAndSaveIssue(sensorContext, ruleKey, inputFile, null, message);
  }

  /**
   * Detect overlapping issues in {@link #candidateIssues}, remove them and report only the remaining issues.
   * Note: this is not prone to race-conditions, as we don't run checks in parallel.
   */
  public void flushIssues() {
    // Each group contains issues where at least one issue overlaps with another issue in the same group.
    // In theory, this could mean that there are issues that do not overlap if we remove e.g. one wide text range.
    // However, in practice we don't expect many issues to overlap, so this is a reasonable approximation.
    var sortedIssueGroups = groupAndPrioritizeOverlappingIssues();
    if (LOG.isDebugEnabled()) {
      logOverlappingIssues(sortedIssueGroups);
    }
    // Raise issues only on a single issue in each group, assuming issues within a group are already sorted by selectivity
    sortedIssueGroups.stream()
      .filter(group -> !group.isEmpty())
      .map(group -> group.get(0))
      .forEach(issue -> createAndSaveIssue(sensorContext, issue.ruleKey, inputFile, issue.textRange, issue.message));

    candidateIssues.clear();
  }

  private static synchronized void createAndSaveIssue(SensorContext sensorContext, RuleKey ruleKey, InputFile inputFile, @Nullable TextRange textRange, String message) {
    // saving issues is not multi-thread safe in sonarAPI (all InputFileContext are using the same sensorContext)
    var issue = sensorContext.newIssue();
    var issueLocation = issue.newLocation()
      .on(inputFile)
      .message(message);
    if (textRange != null) {
      issueLocation.at(textRange);
    }
    issue
      .forRule(ruleKey)
      .at(issueLocation)
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

  private List<List<CandidateIssue>> groupAndPrioritizeOverlappingIssues() {
    var groups = new ArrayList<List<CandidateIssue>>();
    for (var candidateIssue : candidateIssues) {
      groups.stream()
        .filter(group -> group.stream().anyMatch(it -> it.textRange.overlap(candidateIssue.textRange)))
        .findFirst()
        .ifPresentOrElse(group -> group.add(candidateIssue), () -> {
          var newGroup = new ArrayList<CandidateIssue>();
          newGroup.add(candidateIssue);
          groups.add(newGroup);
        });
    }
    for (List<CandidateIssue> group : groups) {
      // This is not the most efficient way, but the sizes of the groups are small enough to not make a difference
      group.sort(comparing(it -> it.selectivity.priority()));
    }
    return groups;
  }

  private void logOverlappingIssues(List<List<CandidateIssue>> issueGroups) {
    issueGroups.stream()
      .filter(group -> group.size() > 1)
      .forEach(group -> LOG.debug("Overlapping issues detected for file {}: Issue {} prioritized over {}",
        inputFile,
        group.get(0).keyAndRange(),
        group.stream().skip(1).map(CandidateIssue::keyAndRange).collect(joining(", "))));
  }

  private record CandidateIssue(
    RuleKey ruleKey,
    Selectivity selectivity,
    TextRange textRange,
    String message) {

    public String keyAndRange() {
      return "[" + ruleKey + ", " + textRange + "]";
    }
  }
}
