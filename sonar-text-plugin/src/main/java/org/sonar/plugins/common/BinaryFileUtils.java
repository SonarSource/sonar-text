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
package org.sonar.plugins.common;

import java.util.regex.Pattern;

public final class BinaryFileUtils {

  private static final Pattern CONTROL_CHARACTERS = Pattern.compile("["
    // Include:
    // 00 Null, 01 Start of Header, 02 Start of Text, 03 End of Text, 04 End of Transmission
    // 05 Enquiry, 06 Acknowledge, 07 Bell, 08 Backspace
    + "\u0000-\u0008"
    // Exclude:
    // 09 Horizontal Tab, 0A Line Feed, 0B Vertical Tab, 0C Form Feed, 0D Carriage Return
    // Include:
    // 0E Shift Out, 0F Shift In, 10 Data Link Escape 11 Device Control 1, 12 Device Control 2, 13 Device Control 3,
    // 14 Device Control 4, 15 Negative Acknowledge, 16 Synchronize, 17 End of Transmission Block, 18 Cancel,
    // 19 End of Medium, 1A Substitute, 1B Escape, 1C File Separator, 1D Group Separator, 1E Record Separator,
    // 1F Unit Separator,
    + "\u000E-\u001F"
    // Exclude all characters >= 20 Space
    + "]");

  private BinaryFileUtils() {
    // utility class
  }

  public static boolean hasControlCharacters(String text) {
    return CONTROL_CHARACTERS.matcher(text).find();
  }

}
