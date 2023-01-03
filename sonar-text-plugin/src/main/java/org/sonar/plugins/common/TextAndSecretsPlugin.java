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
import org.sonar.plugins.secrets.SecretsBuiltInProfileDefinition;
import org.sonar.plugins.secrets.SecretsLanguage;
import org.sonar.plugins.secrets.SecretsRulesDefinition;
import org.sonar.plugins.text.TextBuiltInProfileDefinition;
import org.sonar.plugins.text.TextRuleDefinition;
import org.sonar.plugins.text.TextLanguage;

public class TextAndSecretsPlugin implements Plugin {

  @Override
  public void define(Context context) {
    context.addExtensions(
      // Common
      TextAndSecretsSensor.class,

      // Text
      TextLanguage.class,
      TextBuiltInProfileDefinition.class,
      TextRuleDefinition.class,

      // Secrets
      SecretsLanguage.class,
      SecretsRulesDefinition.class,
      SecretsBuiltInProfileDefinition.class);
  }
}
