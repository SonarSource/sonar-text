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

import java.util.Collection;
import java.util.List;
import org.sonar.plugins.secrets.api.SecretsSpecificationLoader;

public class SecretPreFiltersTest extends AbstractSecretPreFiltersTest {
  public SecretPreFiltersTest() {
    super(SecretsSpecificationLoader.DEFAULT_SPECIFICATION_LOCATION);
  }

  @Override
  protected Collection<String> getExcludedRuleKeys() {
    return List.of();
  }
}
