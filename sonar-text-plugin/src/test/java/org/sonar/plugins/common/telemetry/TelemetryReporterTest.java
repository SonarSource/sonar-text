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

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.sonar.api.SonarRuntime;
import org.sonar.api.batch.sensor.internal.SensorContextTester;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.sonar.plugins.common.TestUtils.SONARCLOUD_RUNTIME;
import static org.sonar.plugins.common.TestUtils.SONARLINT_RUNTIME;
import static org.sonar.plugins.common.TestUtils.SONARQUBE_RUNTIME;
import static org.sonar.plugins.common.TestUtils.SONARQUBE_RUNTIME_WITHOUT_TELEMETRY_SUPPORT;

class TelemetryReporterTest {

  @ParameterizedTest
  @MethodSource
  void shouldReportNumericTelemetry(SonarRuntime runtime) {
    var context = spy(SensorContextTester.create(Path.of(".")).setRuntime(runtime));
    var sensorTelemetry = new TelemetryReporter(context);
    sensorTelemetry.addNumericTelemetry("key1", 5);
    sensorTelemetry.addNumericTelemetry("key1", 3);
    sensorTelemetry.addNumericTelemetry("key2", 1);
    sensorTelemetry.addNumericTelemetry("key3", 0);
    sensorTelemetry.reportTelemetry();

    verify(context).addTelemetryProperty("text.key1", "8");
    verify(context).addTelemetryProperty("text.key2", "1");
    verify(context).addTelemetryProperty("text.key3", "0");
  }

  static List<SonarRuntime> shouldReportNumericTelemetry() {
    return List.of(SONARQUBE_RUNTIME, SONARCLOUD_RUNTIME, SONARLINT_RUNTIME);
  }

  @Test
  void shouldNotReportNumericTelemetryWhenValueIsNegative() {
    var context = spy(SensorContextTester.create(Path.of(".")).setRuntime(SONARQUBE_RUNTIME));
    var sensorTelemetry = new TelemetryReporter(context);
    sensorTelemetry.addNumericTelemetry("key1", -2);
    sensorTelemetry.addNumericTelemetry("key2", 1);
    sensorTelemetry.addNumericTelemetry("key2", -1);
    sensorTelemetry.reportTelemetry();

    verify(context, never()).addTelemetryProperty(eq("text.key1"), any());
    verify(context).addTelemetryProperty("text.key2", "1");
  }

  @Test
  void shouldNotReportNumericTelemetryWhenTelemetryNotSupported() {
    var context = spy(SensorContextTester.create(Path.of(".")).setRuntime(SONARQUBE_RUNTIME_WITHOUT_TELEMETRY_SUPPORT));
    var sensorTelemetry = new TelemetryReporter(context);
    sensorTelemetry.addNumericTelemetry("key1", 5);
    sensorTelemetry.reportTelemetry();

    verify(context, never()).addTelemetryProperty(any(), any());
  }
}
