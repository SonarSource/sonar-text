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
package org.sonar.plugins.text.api;

import java.util.ArrayList;
import java.util.List;
import org.sonar.plugins.common.InputFileContext;
import org.sonar.plugins.common.measures.DurationStatistics;

/**
 * Abstract base class for checks that detect sequences of Unicode codepoints.
 * Subclasses define which codepoints form a detectable sequence and how to report them.
 */
public abstract class AbstractUnicodeSequenceCheck extends TextCheck {

  private DurationStatistics durationStatistics;

  public void initialize(DurationStatistics durationStatistics) {
    this.durationStatistics = durationStatistics;
  }

  @Override
  public void analyze(InputFileContext ctx) {
    durationStatistics.timed(getRuleKey().rule() + DurationStatistics.SUFFIX_TOTAL, () -> analyzeFile(ctx));
  }

  private void analyzeFile(InputFileContext ctx) {
    var lines = ctx.lines();
    for (var lineOffset = 0; lineOffset < lines.size(); lineOffset++) {
      checkLine(ctx, lines.get(lineOffset), lineOffset + 1);
    }
  }

  private void checkLine(InputFileContext ctx, String lineContent, int lineNumber) {
    var currentColumnNumber = 1;
    var sequenceStartColumn = -1;
    var currentSequence = new ArrayList<Integer>();
    var i = 0;
    while (i < lineContent.length()) {
      var newIndex = skipOver(lineContent, i);
      if (newIndex >= 0) {
        if (currentSequence.size() >= minSequenceLength()) {
          reportSequence(ctx, lineNumber, sequenceStartColumn, currentSequence);
        }
        sequenceStartColumn = -1;
        currentSequence = new ArrayList<>();
        i = newIndex;
        currentColumnNumber++;
        continue;
      }

      var currentCodePoint = lineContent.codePointAt(i);
      if (isSequenceChar(currentCodePoint)) {
        if (currentSequence.isEmpty()) {
          sequenceStartColumn = currentColumnNumber;
        }
        currentSequence.add(currentCodePoint);
      } else {
        if (currentSequence.size() >= minSequenceLength()) {
          reportSequence(ctx, lineNumber, sequenceStartColumn, currentSequence);
        }
        sequenceStartColumn = -1;
        currentSequence = new ArrayList<>();
      }
      i += Character.charCount(currentCodePoint);
      currentColumnNumber++;
    }

    if (currentSequence.size() >= minSequenceLength()) {
      reportSequence(ctx, lineNumber, sequenceStartColumn, currentSequence);
    }
  }

  /**
   * Returns true if the given codepoint is part of a detectable sequence.
   */
  protected abstract boolean isSequenceChar(int codePoint);

  /**
   * Called when a sequence of length >= {@link #minSequenceLength()} has been detected.
   * {@code sequenceChars} contains the codepoints that form the sequence.
   */
  protected abstract void reportSequence(InputFileContext ctx, int lineNumber, int startColumn, List<Integer> sequenceChars);

  /**
   * Optionally skip over a special multi-codepoint construct starting at the given index.
   * Return the new index after skipping, or -1 to indicate no skip should happen.
   *
   * @param line the content of the current line being analyzed
   * @param index the current position in the line where a potential skip may start
   * @return the new index after skipping, or -1 if no skip should happen
   * The default implementation never skips.
   */
  protected int skipOver(String line, int index) {
    return -1;
  }

  /**
   * Minimum sequence length required to trigger a report. Default is 2.
   */
  protected int minSequenceLength() {
    return 2;
  }
}
