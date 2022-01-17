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
package org.sonar.plugins.text.core;

import org.sonar.api.resources.AbstractLanguage;

public class TextLanguage extends AbstractLanguage {

  public static final String KEY = "text";
  public static final String NAME = "Text";

  public TextLanguage() {
    super(KEY, NAME);
  }

  @Override
  public String[] getFileSuffixes() {
    // We do not want any files to be associated with this language. The sole purpose of registering the language is to have rules and
    // quality profiles associated to it.
    return new String[]{"sonarShouldNotExistExtension"};
  }
}
