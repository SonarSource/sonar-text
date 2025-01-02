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
package org.sonar.plugins.text;

import org.sonar.api.resources.AbstractLanguage;

public class TextLanguage extends AbstractLanguage {

  public static final String KEY = "text";
  public static final String NAME = "Text";

  public TextLanguage() {
    super(KEY, NAME);
  }

  @Override
  public String[] getFileSuffixes() {
    // We do not want any files to be associated with this language.
    // The sole purpose of registering the language is to have rules and quality profiles associated to it.
    return new String[0];
  }
}
