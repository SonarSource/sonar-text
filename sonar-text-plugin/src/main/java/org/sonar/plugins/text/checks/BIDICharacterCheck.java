/*
 * SonarQube Text Plugin
 * Copyright (C) 2021-2021 SonarSource SA
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
package org.sonar.plugins.text.checks;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Scanner;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.check.Rule;
import org.sonar.plugins.text.api.CheckContext;

@Rule(key = "S6389")
public class BIDICharacterCheck extends AbstractCheck {

  public static final String MESSAGE_FORMAT = "This line contains a bidirectional character in column %d. Make sure that using bidirectional characters is safe here.";

  private static final List<Character> BIDI_FORMATTING_CHARS = List.of(
    '\u202A',	// Left-To-Right Embedding
    '\u202B', // Right-To-Left Embedding
    '\u202D', // Left-To-Right Override
    '\u202E' // Right-To-Left Override
  );
  private static final List<Character> BIDI_ISOLATE_CHARS = List.of(
    '\u2066', // Left-To-Right Isolate
    '\u2067', // Right-To-Left Isolate
    '\u2068'  // First Strong Isolate
  );
  private static final List<Character> BIDI_CHARS = new ArrayList<>(BIDI_FORMATTING_CHARS);
  static {
    BIDI_CHARS.addAll(BIDI_ISOLATE_CHARS);
  }

  private static final char PDF = '\u202C'; // Pop Directional Formatting
  private static final char PDI = '\u2069'; // Pop Directional Isolate

  @Override
  public void analyze(InputFile inputFile) {
    try (InputStream stream = inputFile.inputStream()) {
      analyzeStream(stream, inputFile.charset());
    } catch (IOException e) {
      throw new IllegalStateException("Fail to read file input stream");
    }
  }

  private void analyzeStream(InputStream stream, Charset charset) {
    Scanner scanner = new Scanner(stream, charset);
    int lineNumber = 0;
    while (scanner.hasNextLine()) {
      lineNumber++;
      checkLine(ctx, scanner.nextLine(), lineNumber);
    }
  }

  private static void checkLine(CheckContext ctx, String lineContent, int lineNumber) {
    for (Character bidiChar : BIDI_CHARS) {
      if (lineContent.indexOf(bidiChar) >= 0) {
        // The line contains at least one BIDI character, let's do a more thorough analysis
        checkLineBIDIChars(ctx, lineContent, lineNumber);
        return;
      }
    }
  }

  /**
   * Look for unclose BIDI characters. The rules are as follows:
   * - There has to be one closing PDF for every LRE, RLE, LRO, RLO
   * - There has to be one closing PDI for every LRI, RLI, FSI
   */
  private static void checkLineBIDIChars(CheckContext ctx, String lineContent, int lineNumber) {
    Deque<Integer> unclosedFormattingColumns = new ArrayDeque<Integer>();
    Deque<Integer> unclosedIsolateColumns = new ArrayDeque<Integer>();

    for (int i = 0; i < lineContent.length(); i++) {
      char currentChar = lineContent.charAt(i);
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

  private static void maybeReportOnFirstColumn(CheckContext ctx, int lineNumber, Deque<Integer> unclosedFormattingColumns,
    Deque<Integer> unclosedIsolateColumns) {
    if (unclosedFormattingColumns.isEmpty() && unclosedIsolateColumns.isEmpty()) {
      // Everything was closed correctly. Nothing to report.
      return;
    }

    int columnToReport = 0;
    if (!unclosedFormattingColumns.isEmpty() && !unclosedIsolateColumns.isEmpty()) {
      columnToReport = (unclosedFormattingColumns.getFirst() < unclosedIsolateColumns.getFirst()) ? unclosedFormattingColumns.getFirst() :
        unclosedIsolateColumns.getFirst();
    } else if (!unclosedFormattingColumns.isEmpty()) {
      columnToReport = unclosedFormattingColumns.getFirst();
    } else {
      unclosedIsolateColumns.getFirst();
    }

    ctx.reportLineIssue(lineNumber, String.format(MESSAGE_FORMAT, columnToReport + 1));
  }
}
