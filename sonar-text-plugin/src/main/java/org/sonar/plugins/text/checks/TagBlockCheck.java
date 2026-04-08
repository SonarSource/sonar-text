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
package org.sonar.plugins.text.checks;

import java.util.List;
import org.sonar.check.Rule;
import org.sonar.plugins.common.InputFileContext;
import org.sonar.plugins.text.api.AbstractUnicodeSequenceCheck;

@Rule(key = "S7628")
public class TagBlockCheck extends AbstractUnicodeSequenceCheck {

  public static final String MESSAGE_FORMAT = "This line contains the hidden text \"%s\" starting at column %d. " +
    "Make sure that using Unicode tag blocks is intentional and safe here.";
  private static final int BLACK_FLAG_EMOJI_CODE_POINT = 0x1F3F4;
  private static final int CANCEL_TAG_CODE_POINT = 0xE007F;

  @Override
  protected boolean isSequenceChar(int codePoint) {
    // 0xE0001 is the "language" tag, not used for text
    return codePoint >= 0xE0020 && codePoint <= 0xE007F;
  }

  @Override
  protected void reportSequence(InputFileContext ctx, int lineNumber, int startColumn, List<Integer> sequenceChars) {
    var hiddenText = sequenceChars.stream()
      .map(TagBlockCheck::convertTagCharToChar)
      .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
      .toString();

    if (!hiddenText.isBlank() && hiddenText.length() > 1) {
      ctx.reportTextIssue(getRuleKey(), lineNumber, MESSAGE_FORMAT.formatted(hiddenText, startColumn));
    }
  }

  /**
   * Skip over flag emoji sequences which contain legitimate tag characters.
   * Flag emojis are sequences with a black flag emoji (U+1F3F4), followed by tag characters
   * corresponding to the country code, then an optional cancel tag (U+E007F).
   * For example, for the flag of England: U+1F3F4 U+E0067 U+E0062 U+E0065 U+E006E U+E0067 U+E007F
   */
  @Override
  protected int skipOver(String line, int index) {
    if (line.codePointAt(index) != BLACK_FLAG_EMOJI_CODE_POINT) {
      return -1;
    }
    return getIndexAfterFlagEmoji(line, index);
  }

  private int getIndexAfterFlagEmoji(String text, int startIndex) {
    var currentPosition = startIndex + Character.charCount(BLACK_FLAG_EMOJI_CODE_POINT);
    while (currentPosition < text.length()) {
      var codePoint = text.codePointAt(currentPosition);
      if (!isSequenceChar(codePoint)) {
        // Not a tag character, so it is the end of the sequence
        return currentPosition;
      }
      currentPosition += Character.charCount(codePoint);
      if (codePoint == CANCEL_TAG_CODE_POINT) {
        // Cancel tag marks the end of the sequence
        return currentPosition;
      }
    }
    return currentPosition;
  }

  private static char convertTagCharToChar(int tagCharCodePoint) {
    var charCodePoint = tagCharCodePoint - 0xE0000;
    return (char) charCodePoint;
  }
}
