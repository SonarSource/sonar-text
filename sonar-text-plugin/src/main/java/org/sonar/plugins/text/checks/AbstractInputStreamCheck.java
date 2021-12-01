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

import java.io.IOException;
import java.io.InputStream;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.plugins.text.api.CheckContext;
import org.sonar.plugins.text.api.CommonCheck;
import org.sonar.plugins.text.api.InitContext;

/**
 * Abstract Check to commonly catch possible IOExceptions for InputFiles
 */
public abstract class AbstractInputStreamCheck implements CommonCheck {

  @Override
  public void initialize(InitContext init) {
    init.register((ctx, inputFile) -> {
      try (InputStream inputStream = inputFile.inputStream()) {
        analyzeStream(ctx, inputFile, inputStream);
      } catch (IOException e) {
        // do nothing for now
      }
    });
  }

  abstract void analyzeStream(CheckContext ctx, InputFile inputFile, InputStream stream);
}
