/*
 * SonarQube Text Plugin
 * Copyright (C) 2021-2026 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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

import java.util.Arrays;
import java.util.List;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.plugins.secrets.configuration.SecretsSpecificationContainer;
import org.sonar.plugins.secrets.utils.CheckContainer;

import static org.sonar.plugins.common.TestUtils.SONARQUBE_RUNTIME;
import static org.sonar.plugins.common.TestUtils.activeRules;
import static org.sonar.plugins.common.TestUtils.toRuleKeys;

class TextAndSecretsSensorTest extends AbstractTextAndSecretsSensorTest {

  private static final TestUtils TEST_UTILS = new TestUtils();

  @Override
  protected TextAndSecretsSensor sensor(Check... checks) {
    CheckFactory checkFactory = new CheckFactory(activeRules(toRuleKeys(checks)));
    return new TextAndSecretsSensor(SONARQUBE_RUNTIME, checkFactory, new SecretsSpecificationContainer(), new CheckContainer()) {
      @Override
      protected List<Check> getActiveChecks() {
        return Arrays.stream(checks).toList();
      }
    };
  }

  @Override
  protected TextAndSecretsSensor sensor(SensorContext sensorContext) {
    return new TextAndSecretsSensor(sensorContext.runtime(), new CheckFactory(sensorContext.activeRules()), new SecretsSpecificationContainer(),
      new CheckContainer());
  }

  @Override
  protected TestUtils testUtils() {
    return TEST_UTILS;
  }

  @Override
  protected String sensorName() {
    return "TextAndSecretsSensor";
  }
}
