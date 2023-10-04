/*
 * SonarQube Text Plugin
 * Copyright (C) 2021-2023 SonarSource SA
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

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.InputFile;

public class TextFilePredicate implements FilePredicate {

  private final Set<String> extensions;

  public TextFilePredicate(String... suffixes) {
    extensions = Arrays.stream(suffixes)
      .map(String::trim)
      .filter(value -> !value.isEmpty())
      .collect(Collectors.toSet());
  }

  @Override
  public boolean apply(InputFile inputFile) {
    String filename = inputFile.filename();
    String extension = extension(filename);
    return extensions.contains(extension);
  }

  @Nullable
  public static String extension(String filename) {
    int dotPos = filename.lastIndexOf('.');
    if (dotPos == -1 || dotPos == filename.length() - 1) {
      return null;
    }
    return filename.substring(dotPos + 1).toLowerCase(Locale.ROOT);
  }

}
