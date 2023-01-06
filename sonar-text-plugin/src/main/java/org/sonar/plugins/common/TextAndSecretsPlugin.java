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

import org.sonar.api.Plugin;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.resources.Qualifiers;
import org.sonar.plugins.secrets.SecretsLanguage;
import org.sonar.plugins.secrets.SecretsRulesDefinition;
import org.sonar.plugins.text.TextLanguage;
import org.sonar.plugins.text.TextRuleDefinition;

public class TextAndSecretsPlugin implements Plugin {

  @Override
  public void define(Context context) {
    context.addExtensions(
      // Common
      TextAndSecretsSensor.class,
      PropertyDefinition.builder(TextAndSecretsSensor.EXCLUDED_FILE_SUFFIXES_KEY)
        .defaultValue("")
        .category(TextAndSecretsSensor.TEXT_CATEGORY)
        .name("Additional binary file suffixes")
        .multiValues(true)
        .description("Additional list of binary file suffixes that should not be analyzed with rules targeting text files.")
        .subCategory("General")
        .onQualifiers(Qualifiers.PROJECT)
        .build(),

      // Text
      TextLanguage.class,
      TextRuleDefinition.class,
      TextRuleDefinition.DefaultQualityProfile.class,

      // Secrets
      SecretsLanguage.class,
      SecretsRulesDefinition.class,
      SecretsRulesDefinition.DefaultQualityProfile.class);
  }
}
