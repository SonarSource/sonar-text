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

import java.io.InputStream;
import java.util.List;
import java.util.Scanner;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.check.Rule;
import org.sonar.plugins.text.api.CheckContext;

@Rule(key = "placeholder")
public class BIDICharacterCheck extends AbstractInputStreamCheck {

  public static final String MESSAGE = "Make sure that using bidirectional characters is safe here.";
  private static final List<Character> BIDI_CHARS = List.of(
    '\u061C',
    '\u200E',
    '\u200F',
    '\u202A',
    '\u202B',
    '\u202C',
    '\u202D',
    '\u202E',
    '\u2066',
    '\u2067',
    '\u2068',
    '\u2069'
  );

  @Override
  void analyzeStream(CheckContext ctx, InputFile inputFile, InputStream stream) {
    Scanner scanner = new Scanner(stream, inputFile.charset().name());
    int lineNumber = 0;
    while (scanner.hasNextLine()) {
      lineNumber++;
      checkLine(ctx, scanner.nextLine(), lineNumber);
    }
  }

  private static void checkLine(CheckContext ctx, String lineContent, int lineNumber) {
    for (Character bidiChar : BIDI_CHARS) {
      if (lineContent.indexOf(bidiChar) >= 0) {
        ctx.reportLineIssue(lineNumber, MESSAGE);
      }
    }
  }
}
