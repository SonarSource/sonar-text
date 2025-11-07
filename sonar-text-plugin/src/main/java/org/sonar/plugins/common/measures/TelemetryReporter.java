/*
 * SonarQube Text Plugin
 * Copyright (C) 2021-2025 SonarSource Sàrl
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
package org.sonar.plugins.common.measures;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.utils.Version;

public class TelemetryReporter {

  // `.` should be used for telemetry groups and every Text key should start with `text.`
  public static final String KEY_PREFIX = "text.";
  private static final Version TELEMETRY_SUPPORTED_API_VERSION = Version.create(10, 9);

  private final SensorContext sensorContext;
  private final Map<String, Integer> numericMeasures;
  private long startRecordingTimeMs;

  public TelemetryReporter(SensorContext sensorContext) {
    this.sensorContext = sensorContext;
    this.numericMeasures = new HashMap<>();
  }

  public void startRecordingSensorTime() {
    startRecordingTimeMs = System.currentTimeMillis();
  }

  public void endRecordingSensorTime(String suffix) {
    long endRecordingTimeMs = System.currentTimeMillis();
    long durationMs = endRecordingTimeMs - startRecordingTimeMs;
    var key = "sensor_time_ms_" + suffix.toLowerCase(Locale.ROOT);
    addNumericMeasure(key, (int) durationMs);
  }

  public TelemetryReporter addNumericMeasure(String key, int value) {
    if (value < 0) {
      return this;
    }
    key = KEY_PREFIX + key;
    numericMeasures.merge(key, value, Integer::sum);
    return this;
  }

  public void report() {
    var isTelemetrySupported = sensorContext.runtime().getApiVersion().isGreaterThanOrEqual(TELEMETRY_SUPPORTED_API_VERSION);
    if (isTelemetrySupported) {
      // addTelemetryProperty is added in 10.9:
      // https://github.com/SonarSource/sonar-plugin-api/releases/tag/10.9.0.2362
      numericMeasures.forEach((key, numericValue) -> sensorContext.addTelemetryProperty(key, numericValue.toString()));
    }
  }
}
