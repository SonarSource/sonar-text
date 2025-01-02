/*
 * SonarQube Text Plugin
 * Copyright (C) 2021-2025 SonarSource SA
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
package org.sonar.plugins.text.checks;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.plugins.common.InputFileContext;
import org.sonar.plugins.text.api.TextCheck;

@Rule(key = "S6389")
public class BIDICharacterCheck extends TextCheck {

  public static final String MESSAGE_FORMAT = "This line contains a bidirectional character in column %d. Make sure that using bidirectional characters is safe here.";

  private static final List<Character> BIDI_FORMATTING_CHARS = List.of(
    '\u202A', // Left-To-Right Embedding
    '\u202B', // Right-To-Left Embedding
    '\u202D', // Left-To-Right Override
    '\u202E' // Right-To-Left Override
  );
  private static final List<Character> BIDI_ISOLATE_CHARS = List.of(
    '\u2066', // Left-To-Right Isolate
    '\u2067', // Right-To-Left Isolate
    '\u2068' // First Strong Isolate
  );
  private static final List<Character> BIDI_CHARS = new ArrayList<>(BIDI_FORMATTING_CHARS);
  static {
    BIDI_CHARS.addAll(BIDI_ISOLATE_CHARS);
  }

  private static final char PDF = '\u202C'; // Pop Directional Formatting
  private static final char PDI = '\u2069'; // Pop Directional Isolate

  @Override
  public void analyze(InputFileContext ctx) {
    List<String> lines = ctx.lines();
    for (var lineOffset = 0; lineOffset < lines.size(); lineOffset++) {
      checkLine(ctx, lines.get(lineOffset), lineOffset + 1);
    }
  }

  private void checkLine(InputFileContext ctx, String lineContent, int lineNumber) {
    for (Character bidiChar : BIDI_CHARS) {
      if (lineContent.indexOf(bidiChar) >= 0) {
        // The line contains at least one BIDI character, let's do a more thorough analysis
        checkLineBIDIChars(ctx, lineContent, lineNumber);
        return;
      }
    }
  }

  /**
   * Look for unclosed BIDI characters. The rules are as follows:
   * - There has to be one closing PDF for every LRE, RLE, LRO, RLO
   * - There has to be one closing PDI for every LRI, RLI, FSI
   */
  private void checkLineBIDIChars(InputFileContext ctx, String lineContent, int lineNumber) {
    Deque<Integer> unclosedFormattingColumns = new ArrayDeque<>();
    Deque<Integer> unclosedIsolateColumns = new ArrayDeque<>();

    for (var i = 0; i < lineContent.length(); i++) {
      var currentChar = lineContent.charAt(i);
      if (BIDI_FORMATTING_CHARS.contains(currentChar)) {
        unclosedFormattingColumns.push(i);
      } else if (BIDI_ISOLATE_CHARS.contains(currentChar)) {
        unclosedIsolateColumns.push(i);
      } else if (currentChar == PDF && !unclosedFormattingColumns.isEmpty()) {
        unclosedFormattingColumns.pop();
      } else if (currentChar == PDI && !unclosedIsolateColumns.isEmpty()) {
        unclosedIsolateColumns.pop();
      }
    }

    maybeReportOnFirstColumn(ctx, lineNumber, unclosedFormattingColumns, unclosedIsolateColumns);
  }

  private void maybeReportOnFirstColumn(InputFileContext ctx, int lineNumber, Deque<Integer> unclosedFormattingColumns,
    Deque<Integer> unclosedIsolateColumns) {
    if (unclosedFormattingColumns.isEmpty() && unclosedIsolateColumns.isEmpty()) {
      // Everything was closed correctly. Nothing to report.
      return;
    }

    var columnToReport = 0;
    if (!unclosedFormattingColumns.isEmpty() && !unclosedIsolateColumns.isEmpty()) {
      if (unclosedFormattingColumns.getFirst() < unclosedIsolateColumns.getFirst()) {
        columnToReport = unclosedFormattingColumns.getFirst();
      } else {
        columnToReport = unclosedIsolateColumns.getFirst();
      }
    } else if (!unclosedFormattingColumns.isEmpty()) {
      columnToReport = unclosedFormattingColumns.getFirst();
    } else {
      columnToReport = unclosedIsolateColumns.getFirst();
    }

    ctx.reportTextIssue(getRuleKey(), lineNumber, String.format(MESSAGE_FORMAT, columnToReport + 1));
  }
}
