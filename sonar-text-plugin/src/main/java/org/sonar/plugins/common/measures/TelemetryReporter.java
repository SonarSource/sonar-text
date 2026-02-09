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
package org.sonar.plugins.common.measures;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.utils.Version;

public class TelemetryReporter {

  // `.` should be used for telemetry groups and every Text key should start with `text.`
  public static final String KEY_PREFIX = "text.";
  private static final Version TELEMETRY_SUPPORTED_API_VERSION = Version.create(10, 9);
  private static final int MAX_BYTES = 16 * 1024;
  private static final Logger LOG = LoggerFactory.getLogger(TelemetryReporter.class);
  // Property key to enable log, currently using the DurationStatistics one
  private static final String PROPERTY_KEY = "sonar.text.duration.statistics";

  private final SensorContext sensorContext;
  private final Map<String, Integer> numericMeasures;
  private final Map<String, String> stringMeasures;
  private long startRecordingTimeMs;
  private final boolean shouldLogTelemetry;

  public TelemetryReporter(SensorContext sensorContext) {
    this.sensorContext = sensorContext;
    this.numericMeasures = new HashMap<>();
    this.stringMeasures = new HashMap<>();
    this.shouldLogTelemetry = sensorContext.config().getBoolean(PROPERTY_KEY).orElse(false);
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

  public void addListAsStringMeasure(String key, List<String> list) {
    if (list.isEmpty()) {
      return;
    }
    String jsonString = list.stream()
      .map(s -> "\"" + s + "\"")
      .collect(Collectors.joining(", ", "[", "]"));
    addStringMeasure(key, jsonString);
  }

  public void addStringMeasure(String key, String value) {
    if (value.isEmpty() || value.length() > MAX_BYTES) {
      LOG.debug("Failed to add telemetry with key {}, value empty or bigger then specified string limit.", key);
      return;
    }
    key = KEY_PREFIX + key;
    stringMeasures.put(key, value);
  }

  public void report() {
    var isTelemetrySupported = sensorContext.runtime().getApiVersion().isGreaterThanOrEqual(TELEMETRY_SUPPORTED_API_VERSION);
    if (isTelemetrySupported) {
      // addTelemetryProperty is added in 10.9:
      // https://github.com/SonarSource/sonar-plugin-api/releases/tag/10.9.0.2362
      numericMeasures.forEach((key, numericValue) -> logAndAddTelemetry(key, numericValue.toString()));
      stringMeasures.forEach(this::logAndAddTelemetry);
    }
  }

  private void logAndAddTelemetry(String key, String value) {
    if (shouldLogTelemetry) {
      LOG.debug("Reporting telemetry: {}={}", key, value);
    }
    sensorContext.addTelemetryProperty(key, value);
  }
}
