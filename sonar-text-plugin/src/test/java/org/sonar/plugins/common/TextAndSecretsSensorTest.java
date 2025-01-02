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
package org.sonar.plugins.common;

import java.util.Collections;
import java.util.List;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.sensor.SensorContext;

import static org.sonar.plugins.common.TestUtils.activeRules;

public class TextAndSecretsSensorTest extends AbstractTextAndSecretsSensorTest {

  private static final TestUtils TEST_UTILS = new TestUtils();

  @Override
  protected TextAndSecretsSensor sensor(Check check) {
    CheckFactory checkFactory = new CheckFactory(activeRules(check.getRuleKey().toString()));
    return new TextAndSecretsSensor(checkFactory) {
      @Override
      protected List<Check> getActiveChecks() {
        return Collections.singletonList(check);
      }
    };
  }

  @Override
  protected TextAndSecretsSensor sensor(SensorContext sensorContext) {
    return new TextAndSecretsSensor(new CheckFactory(sensorContext.activeRules()));
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
