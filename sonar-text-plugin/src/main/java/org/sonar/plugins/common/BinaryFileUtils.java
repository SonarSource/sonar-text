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
package org.sonar.plugins.common;

import java.util.regex.Pattern;

public final class BinaryFileUtils {

  private static final Pattern NON_TEXT_CHARACTERS = Pattern.compile("["
    // Non text characters are a subset of the non-printable characters < 0x20 (Space)
    // Legend:
    // (N) Non text characters
    // (T) text characters intentionally writen by the developer
    // (~) accepted as text characters but inserted by mistake using a copy/paste or a generation tool
    //
    + "\u0001-\u0007"
    // (~) 0x00 Null
    // (N) 0x01 Start of Header
    // (N) 0x02 Start of Text
    // (N) 0x03 End of Text
    // (N) 0x04 End of Transmission
    // (N) 0x05 Enquiry
    // (N) 0x06 Acknowledge
    // (N) 0x07 Bell
    // (~) 0x08 Backspace
    // (T) 0x09 Horizontal Tab
    // (T) 0x0A Line Feed
    // (~) 0x0B Vertical Tab
    // (~) 0x0C Form Feed
    // (T) 0x0D Carriage Return
    + "\u000E-\u001A"
    // (N) 0x0E Shift Out
    // (N) 0x0F Shift In
    // (N) 0x10 Data Link Escape
    // (N) 0x11 Device Control 1
    // (N) 0x12 Device Control 2
    // (N) 0x13 Device Control 3
    // (N) 0x14 Device Control 4
    // (N) 0x15 Negative Acknowledge
    // (N) 0x16 Synchronize
    // (N) 0x17 End of Transmission Block
    // (N) 0x18 Cancel
    // (N) 0x19 End of Medium
    // (N) 0x1A Substitute
    // (~) 0x1B Escape
    + "\u001C-\u001F"
    // (N) 0x1C File Separator
    // (N) 0x1D Group Separator
    // (N) 0x1E Record Separator
    // (N) 0x1F Unit Separator
    + "]");

  private BinaryFileUtils() {
    // utility class
  }

  public static boolean hasNonTextCharacters(String text) {
    return NON_TEXT_CHARACTERS.matcher(text).find();
  }

}
