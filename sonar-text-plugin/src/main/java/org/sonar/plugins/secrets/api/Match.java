/*
 * SonarQube Text Plugin
 * Copyright (C) 2021-2024 SonarSource SA
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
package org.sonar.plugins.secrets.api;

public class Match {
  private final String text;
  private final int fileStartOffset;
  private final int fileEndOffset;

  public Match(String text, int fileStartOffset, int fileEndOffset) {
    this.text = text;
    this.fileStartOffset = fileStartOffset;
    this.fileEndOffset = fileEndOffset;
  }

  public String getText() {
    return text;
  }

  public int getFileStartOffset() {
    return fileStartOffset;
  }

  public int getFileEndOffset() {
    return fileEndOffset;
  }

  @Override
  public String toString() {
    return "Match{" +
      "text='" + text + '\'' +
      ", fileStartOffset=" + fileStartOffset +
      ", fileEndOffset=" + fileEndOffset +
      '}';
  }
}
