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

import java.util.ArrayList;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.plugins.common.InputFileContext;
import org.sonar.plugins.common.measures.DurationStatistics;
import org.sonar.plugins.text.api.TextCheck;

@Rule(key = "S7628")
public class TagBlockCheck extends TextCheck {

  public static final String MESSAGE_FORMAT = "This line contains the hidden text \"%s\" starting at column %d. " +
    "Make sure that using Unicode tag blocks is intentional and safe here.";
  private static final int BLACK_FLAG_EMOJI_CODE_POINT = 0x1F3F4;
  private static final int CANCEL_TAG_CODE_POINT = 0xE007F;
  private DurationStatistics durationStatistics;

  public void initialize(DurationStatistics durationStatistics) {
    this.durationStatistics = durationStatistics;
  }

  @Override
  public void analyze(InputFileContext ctx) {
    durationStatistics.timed(getRuleKey().rule() + DurationStatistics.SUFFIX_TOTAL, new TagBlockFileAnalyzer(ctx)::analyze);
  }

  /**
   * Makes the check stateless to allow parallel analysis of files.
   */
  private class TagBlockFileAnalyzer {
    private final InputFileContext ctx;
    // Not initialized until a tag sequence is found to avoid unnecessary allocations
    private List<Integer> tagSequence;
    private Integer tagSequenceStartIndex;
    private Integer currentColumnNumber;

    public TagBlockFileAnalyzer(InputFileContext ctx) {
      this.ctx = ctx;
    }

    public void analyze() {
      List<String> lines = ctx.lines();
      for (var lineOffset = 0; lineOffset < lines.size(); lineOffset++) {
        checkLine(ctx, lines.get(lineOffset), lineOffset + 1);
      }
    }

    private void checkLine(InputFileContext ctx, String lineContent, int lineNumber) {
      currentColumnNumber = 1;
      var i = 0;
      while (i < lineContent.length()) {
        var currentCodePoint = lineContent.codePointAt(i);

        // Skip flag emoji sequences which contain legitimate tag characters
        if (isBlackFlagEmoji(currentCodePoint)) {
          i = getIndexAfterFlagEmoji(lineContent, i);
          currentColumnNumber++;
          continue;
        }

        if (isTag(currentCodePoint)) {
          if (!isTagSequenceOngoing()) {
            initTagSequence(i);
          }
          tagSequence.add(currentCodePoint);
        } else if (isTagSequenceOngoing()) {
          flushTagSequence(ctx, lineNumber);
        }
        i += Character.charCount(lineContent.codePointAt(i));
        currentColumnNumber++;
      }

      // Report tag sequence at end of line if it exists
      if (isTagSequenceOngoing()) {
        flushTagSequence(ctx, lineNumber);
      }
    }

    private void initTagSequence(int startIndex) {
      tagSequenceStartIndex = startIndex;
      if (tagSequence == null) {
        tagSequence = new ArrayList<>();
      }
    }

    private boolean isTagSequenceOngoing() {
      return tagSequenceStartIndex != null;
    }

    /**
     * Skip over a flag emoji sequence in the text.
     * Flag emojis are sequences with a black flag emoji (U+1F3F4), followed by tag characters corresponding
     * to the country code, then a cancel tag (U+E007F).
     * For example, for the flag of England: U+1F3F4 U+E0067 U+E0062 U+E0065 U+E006E U+E0067 U+E007F
     */
    private int getIndexAfterFlagEmoji(String text, int startIndex) {
      var positionAfterBlackFlag = startIndex + Character.charCount(BLACK_FLAG_EMOJI_CODE_POINT);
      var endPosition = positionAfterBlackFlag;
      var foundCancelTag = false;
      while (endPosition < text.length()) {
        var codePoint = text.codePointAt(endPosition);
        if (!isTag(codePoint)) {
          break;
        }
        endPosition += Character.charCount(codePoint);
        if (codePoint == CANCEL_TAG_CODE_POINT) {
          foundCancelTag = true;
          break;
        }
      }
      return foundCancelTag ? endPosition : positionAfterBlackFlag;
    }

    private void flushTagSequence(InputFileContext ctx, int lineNumber) {
      var hiddenText = tagSequence.stream()
        .map(TagBlockCheck::convertTagCharToChar)
        .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
        .toString();

      if (!hiddenText.isBlank() && hiddenText.length() > 1) {
        var tagSequenceStartColumnNumber = currentColumnNumber - tagSequence.size();
        ctx.reportTextIssue(
          getRuleKey(),
          lineNumber,
          MESSAGE_FORMAT.formatted(hiddenText, tagSequenceStartColumnNumber));
      }

      tagSequence.clear();
      tagSequenceStartIndex = null;
    }
  }

  private static boolean isTag(int charCodePoint) {
    // 0xE0001 is the "language" tag, not used for text
    return charCodePoint >= 0xE0020 && charCodePoint <= 0xE007F;
  }

  private static boolean isBlackFlagEmoji(int charCodePoint) {
    return charCodePoint == BLACK_FLAG_EMOJI_CODE_POINT;
  }

  private static char convertTagCharToChar(int tagCharCodePoint) {
    var charCodePoint = tagCharCodePoint - 0xE0000;
    return (char) charCodePoint;
  }
}
