/*
 * SonarQube Text Plugin
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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
import org.sonar.api.config.PropertyDefinition.ConfigScope;
import org.sonar.plugins.common.predicates.TextAndSecretsPredicates;
import org.sonar.plugins.common.warnings.DefaultAnalysisWarningsWrapper;
import org.sonar.plugins.secrets.SecretsLanguage;
import org.sonar.plugins.secrets.SecretsRulesDefinition;
import org.sonar.plugins.secrets.configuration.SecretsSpecificationContainer;
import org.sonar.plugins.secrets.utils.CheckContainer;
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
      SecretsRulesDefinition.DefaultQualityProfile.class,
      SecretsSpecificationContainer.class,
      CheckContainer.class);

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
        .onConfigScopes(ConfigScope.PROJECT)
        .category(TextAndSecretsSensor.TEXT_CATEGORY)
        .subCategory(GENERAL_SUBCATEGORY)
        .build(),

      PropertyDefinition.builder(TextAndSecretsPredicates.INCLUSIONS_ACTIVATION_KEY)
        .index(2)
        .defaultValue(String.valueOf(TextAndSecretsPredicates.INCLUSIONS_ACTIVATION_DEFAULT_VALUE))
        .name("Activate inclusion of custom file path patterns")
        .description("Disabling custom file path patterns ensures that only files associated to a language will get analyzed.")
        .type(PropertyType.BOOLEAN)
        .onConfigScopes(ConfigScope.PROJECT)
        .category(TextAndSecretsSensor.TEXT_CATEGORY)
        .subCategory(GENERAL_SUBCATEGORY)
        .build(),

      PropertyDefinition.builder(TextAndSecretsPredicates.EXCLUDED_FILE_SUFFIXES_KEY)
        .defaultValue(TextAndSecretsPredicates.EXCLUDED_FILE_SUFFIXES_DEFAULT_VALUE)
        .category(TextAndSecretsSensor.TEXT_CATEGORY)
        .name("Secrets analysis excluded file suffixes")
        .multiValues(true)
        .description("List of file suffixes that should not be analyzed with the secrets analysis.")
        .subCategory(GENERAL_SUBCATEGORY)
        .onConfigScopes(ConfigScope.PROJECT)
        .build(),

      PropertyDefinition.builder(TextAndSecretsPredicates.DEPRECATED_EXCLUDED_BINARY_FILE_SUFFIXES_KEY)
        .defaultValue("")
        .category(TextAndSecretsSensor.TEXT_CATEGORY)
        .name("(Deprecated) Additional binary file suffixes")
        .multiValues(true)
        .description(
          "Deprecated. Please migrate excluded file suffixes to '%s'. Additional list of binary file suffixes that should not be analyzed with rules targeting text files."
            .formatted(TextAndSecretsPredicates.EXCLUDED_FILE_SUFFIXES_KEY))
        .subCategory(GENERAL_SUBCATEGORY)
        .onConfigScopes(ConfigScope.PROJECT)
        .build(),

      PropertyDefinition.builder(TextAndSecretsPredicates.TEXT_INCLUSIONS_KEY)
        .defaultValue(TextAndSecretsPredicates.TEXT_INCLUSIONS_DEFAULT_VALUE)
        .category(TextAndSecretsSensor.TEXT_CATEGORY)
        .name("List of file path patterns to include")
        .multiValues(true)
        .description("List of file path patterns that should be analyzed with rules targeting text files (ie. Secret rules, BIDI rule), " +
          "in addition to those associated to a language. This is only applied when the scanner detects a git repository. ")
        .subCategory(GENERAL_SUBCATEGORY)
        .onConfigScopes(ConfigScope.PROJECT)
        .build(),

      PropertyDefinition.builder(TextAndSecretsSensor.DISABLE_ENTROPY_FILTER_KEY)
        .defaultValue(String.valueOf(TextAndSecretsSensor.DISABLE_ENTROPY_FILTER_DEFAULT_VALUE))
        .category(TextAndSecretsSensor.TEXT_CATEGORY)
        .name("Disable the entropy filter for secret detection")
        .description("When enabled, low-entropy matches that would normally be filtered out are raised as low-confidence issues. " +
          "This helps surface potential fake or example secrets in benchmark or evaluation projects. " +
          "Other post-filters are unaffected.")
        .type(PropertyType.BOOLEAN)
        .onConfigScopes(ConfigScope.PROJECT)
        .subCategory(GENERAL_SUBCATEGORY)
        .build(),

      PropertyDefinition.builder(TextAndSecretsSensor.DISABLE_TEST_FILE_DETECTION_KEY)
        .defaultValue(String.valueOf(TextAndSecretsSensor.DISABLE_TEST_FILE_DETECTION_DEFAULT_VALUE))
        .category(TextAndSecretsSensor.TEXT_CATEGORY)
        .name("Disable automatic test-file detection for secret detection")
        .description("When enabled, automatically detected test files are analyzed and findings are raised as low-confidence issues. " +
          "This property has no effect on files explicitly classified as tests through the \"sonar.tests\" property.")
        .type(PropertyType.BOOLEAN)
        .onConfigScopes(ConfigScope.PROJECT)
        .subCategory(GENERAL_SUBCATEGORY)
        .build());
  }
}
