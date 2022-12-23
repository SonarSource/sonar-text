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

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.plugins.secrets.NormalizedInputFile;

import static java.nio.charset.StandardCharsets.UTF_8;

public class TestUtils {

  public static NormalizedInputFile aNormalizedInputFile(String fileContent) {
    return aNormalizedInputFile(fileContent, "fileName");
  }

  public static NormalizedInputFile aNormalizedInputFile(String fileContent, String fileName) {
    String content = fileContent.replaceAll("\\r\\n?", "\n");
    return new NormalizedInputFile(new TestInputFileBuilder("", fileName)
      .setContents(content)
      .setCharset(UTF_8)
      .setLanguage("java")
      .build(), content);
  }

  public static NormalizedInputFile readFileAndNormalize(String path, Charset encoding)
    throws IOException {
    byte[] encoded = Files.readAllBytes(Paths.get(path));
    return aNormalizedInputFile(new String(encoded, encoding));
  }
}
