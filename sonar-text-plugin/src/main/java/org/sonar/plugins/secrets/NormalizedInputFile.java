/*
 * SonarQube Text Plugin
 * Copyright (C) 2021-2022 SonarSource SA
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
package org.sonar.plugins.secrets;

import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextPointer;
import org.sonar.api.batch.fs.TextRange;

import static java.util.Objects.requireNonNull;

public class NormalizedInputFile {
  private static final char LINE_FEED = '\n';

  private final InputFile file;
  private final String content;

  public NormalizedInputFile(InputFile file, String content) {
    this.file = file;
    this.content = content;
  }

  public String getContent() {
    return content;
  }

  public InputFile getFile() {
    return file;
  }

  public TextRange newTextRangeFromFileOffsets(int startOffset, int endOffset) {
    int lineNumber = 1;
    TextPointer startPointer = null;
    int currentLineStartOffset = 0;
    int charOffset = 0;
    for (; charOffset < content.length(); charOffset++) {
      if (charOffset == startOffset) {
        startPointer = file.newPointer(lineNumber, startOffset - currentLineStartOffset);
      }
      if (charOffset == endOffset) {
        TextPointer endPointer = file.newPointer(lineNumber, endOffset - currentLineStartOffset);
        return file.newRange(requireNonNull(startPointer), endPointer);
      }

      if (content.charAt(charOffset) == LINE_FEED) {
        lineNumber++;
        currentLineStartOffset = charOffset + 1;
      }
    }
    // check again in case the end offset is after the last character
    if (startPointer != null && charOffset == endOffset) {
      TextPointer endPointer = file.newPointer(lineNumber, endOffset - currentLineStartOffset);
      return file.newRange(requireNonNull(startPointer), endPointer);
    }
    // should not happen as input parameters are supposed to be valid offsets
    throw new IllegalStateException("Invalid offsets: startOffset=" + startOffset + ", endOffset=" + endOffset);
  }
}
