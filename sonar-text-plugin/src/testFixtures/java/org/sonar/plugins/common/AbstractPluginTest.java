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

import org.junit.jupiter.api.Test;
import org.sonar.api.Plugin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.plugins.common.TestUtils.SONARQUBE_RUNTIME;

public abstract class AbstractPluginTest {

  @Test
  void shouldDefineExtensions() {
    Plugin.Context context = new Plugin.Context(SONARQUBE_RUNTIME);
    Plugin plugin = getPlugin();
    plugin.define(context);
    assertThat(context.getExtensions()).hasSize(getExpectedExtensionCount());
  }

  protected abstract Plugin getPlugin();

  protected abstract int getExpectedExtensionCount();
}
