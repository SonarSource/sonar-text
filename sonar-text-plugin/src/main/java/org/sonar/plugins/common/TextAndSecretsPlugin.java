/*
 * SonarQube Text Plugin
 * Copyright (C) 2021-2024 SonarSource SA
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

import java.util.List;
import org.sonar.api.Plugin;
import org.sonar.api.PropertyType;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.resources.Qualifiers;
import org.sonar.plugins.common.warnings.DefaultAnalysisWarningsWrapper;
import org.sonar.plugins.secrets.SecretsLanguage;
import org.sonar.plugins.secrets.SecretsRulesDefinition;
import org.sonar.plugins.text.TextLanguage;
import org.sonar.plugins.text.TextRuleDefinition;

public class TextAndSecretsPlugin implements Plugin {

  private static final String GENERAL_SUBCATEGORY = "General";

  @Override
  public void define(Context context) {
    context.addExtensions(
      // Common
      TextAndSecretsSensor.class,
      DefaultAnalysisWarningsWrapper.class,

      // Text
      TextLanguage.class,
      TextRuleDefinition.class,
      TextRuleDefinition.DefaultQualityProfile.class,

      // Secrets
      SecretsLanguage.class,
      SecretsRulesDefinition.class,
      SecretsRulesDefinition.DefaultQualityProfile.class);

    context.addExtensions(createUIProperties());
  }

  public static List<PropertyDefinition> createUIProperties() {
    return List.of(
      PropertyDefinition.builder(TextAndSecretsSensor.ANALYZER_ACTIVATION_KEY)
        .index(1)
        .defaultValue(String.valueOf(TextAndSecretsSensor.ANALYZER_ACTIVATION_DEFAULT_VALUE))
        .name("Activate Secrets Analysis")
        .description("Disabling Secrets analysis ensures that no files are analyzed for containing secrets.")
        .type(PropertyType.BOOLEAN)
        .onQualifiers(Qualifiers.PROJECT)
        .category(TextAndSecretsSensor.TEXT_CATEGORY)
        .subCategory(GENERAL_SUBCATEGORY)
        .build(),

      PropertyDefinition.builder(TextAndSecretsSensor.INCLUSIONS_ACTIVATION_KEY)
        .index(2)
        .defaultValue(String.valueOf(TextAndSecretsSensor.INCLUSIONS_ACTIVATION_DEFAULT_VALUE))
        .name("Activate inclusion of custom file path patterns")
        .description("Disabling custom file path patterns ensures that only files associated to a language will get analyzed.")
        .type(PropertyType.BOOLEAN)
        .onQualifiers(Qualifiers.PROJECT)
        .category(TextAndSecretsSensor.TEXT_CATEGORY)
        .subCategory(GENERAL_SUBCATEGORY)
        .build(),

      PropertyDefinition.builder(TextAndSecretsSensor.EXCLUDED_FILE_SUFFIXES_KEY)
        .defaultValue("")
        .category(TextAndSecretsSensor.TEXT_CATEGORY)
        .name("Additional binary file suffixes")
        .multiValues(true)
        .description("Additional list of binary file suffixes that should not be analyzed with rules targeting text files.")
        .subCategory(GENERAL_SUBCATEGORY)
        .onQualifiers(Qualifiers.PROJECT)
        .build(),

      PropertyDefinition.builder(TextAndSecretsSensor.TEXT_INCLUSIONS_KEY)
        .defaultValue(TextAndSecretsSensor.TEXT_INCLUSIONS_DEFAULT_VALUE)
        .category(TextAndSecretsSensor.TEXT_CATEGORY)
        .name("List of file path patterns to include")
        .multiValues(true)
        .description("List of file path patterns that should be analyzed with rules targeting text files (ie. Secret rules, BIDI rule), " +
          "in addition to those associated to a language. This is only applied when the scanner detects a git repository. " +
          "It's not possible to analyze files or directories starting with a dot on UNIX systems.")
        .subCategory(GENERAL_SUBCATEGORY)
        .onQualifiers(Qualifiers.PROJECT)
        .build());
  }
}
