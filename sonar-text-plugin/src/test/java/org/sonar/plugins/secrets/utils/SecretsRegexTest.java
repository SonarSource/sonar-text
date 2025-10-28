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
package org.sonar.plugins.secrets.utils;

import java.util.Set;
import org.sonar.plugins.secrets.SecretsSpecificationFilesDefinition;
import org.sonar.plugins.secrets.api.SecretsSpecificationLoader;

public class SecretsRegexTest extends AbstractSecretsRegexTest {
  public SecretsRegexTest() {
    super(SecretsSpecificationLoader.DEFAULT_SPECIFICATION_LOCATION, "baseline.yaml");
  }

  @Override
  protected Set<String> getSpecificationFiles() {
    return SecretsSpecificationFilesDefinition.existingSecretSpecifications();
  }
}
