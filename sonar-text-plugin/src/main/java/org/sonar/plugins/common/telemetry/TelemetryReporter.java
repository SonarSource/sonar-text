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
package org.sonar.plugins.common.telemetry;

import java.util.HashMap;
import java.util.Map;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.utils.Version;

public class TelemetryReporter {

  // `.` should be used for telemetry groups and every Text key should start with `text.`
  public static final String KEY_PREFIX = "text.";
  private static final Version TELEMETRY_SUPPORTED_API_VERSION = Version.create(10, 9);

  private final SensorContext sensorContext;
  private final Map<String, Integer> numericTelemetry;

  public TelemetryReporter(SensorContext sensorContext) {
    this.sensorContext = sensorContext;
    this.numericTelemetry = new HashMap<>();
  }

  public void addNumericTelemetry(String key, int value) {
    if (value < 0) {
      return;
    }
    key = KEY_PREFIX + key;
    numericTelemetry.merge(key, value, Integer::sum);
  }

  public void reportTelemetry() {
    var isTelemetrySupported = sensorContext.runtime().getApiVersion().isGreaterThanOrEqual(TELEMETRY_SUPPORTED_API_VERSION);
    if (isTelemetrySupported) {
      // addTelemetryProperty is added in 10.9:
      // https://github.com/SonarSource/sonar-plugin-api/releases/tag/10.9.0.2362
      numericTelemetry.forEach((key, numericValue) -> sensorContext.addTelemetryProperty(key, numericValue.toString()));
    }
  }
}
