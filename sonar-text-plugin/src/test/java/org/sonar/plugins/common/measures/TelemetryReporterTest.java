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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.event.Level;
import org.sonar.api.SonarRuntime;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;

import static org.assertj.core.api.Assertions.assertThat;
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

  @RegisterExtension
  public LogTesterJUnit5 logTester = new LogTesterJUnit5().setLevel(Level.DEBUG);

  @ParameterizedTest
  @MethodSource("provideTelemetrySonarRuntime")
  void shouldReportNumericTelemetry(SonarRuntime runtime) {
    var context = spy(SensorContextTester.create(Path.of(".")).setRuntime(runtime));
    var sensorTelemetry = new TelemetryReporter(context);
    sensorTelemetry.addNumericMeasure("key1", 5);
    sensorTelemetry.addNumericMeasure("key1", 3);
    sensorTelemetry.addNumericMeasure("key2", 1);
    sensorTelemetry.addNumericMeasure("key3", 0);
    sensorTelemetry.report();

    verify(context).addTelemetryProperty("text.key1", "8");
    verify(context).addTelemetryProperty("text.key2", "1");
    verify(context).addTelemetryProperty("text.key3", "0");
  }

  @ParameterizedTest
  @MethodSource("provideTelemetrySonarRuntime")
  void shouldReportStringTelemetry(SonarRuntime runtime) {
    var context = spy(SensorContextTester.create(Path.of(".")).setRuntime(runtime));
    var sensorTelemetry = new TelemetryReporter(context);
    sensorTelemetry.addStringMeasure("key1", "value1");
    sensorTelemetry.addStringMeasure("key1", "value2");
    sensorTelemetry.addStringMeasure("key2", "value3");
    sensorTelemetry.addStringMeasure("key3", "value4");
    sensorTelemetry.report();

    verify(context).addTelemetryProperty("text.key1", "value2");
    verify(context).addTelemetryProperty("text.key2", "value3");
    verify(context).addTelemetryProperty("text.key3", "value4");
  }

  @ParameterizedTest
  @MethodSource("provideTelemetrySonarRuntime")
  void shouldReportListAsStringTelemetry(SonarRuntime runtime) {
    var context = spy(SensorContextTester.create(Path.of(".")).setRuntime(runtime));
    var sensorTelemetry = new TelemetryReporter(context);
    sensorTelemetry.addListAsStringMeasure("key1", List.of("", "value1", "value2"));
    sensorTelemetry.addListAsStringMeasure("key2", List.of("", "value1", "value2"));
    sensorTelemetry.addListAsStringMeasure("key2", List.of("value1", "", "value2"));
    sensorTelemetry.addListAsStringMeasure("key3", List.of("value1", "value2", ""));
    sensorTelemetry.addListAsStringMeasure("key4", List.of("value1", "value2", "value3"));
    sensorTelemetry.addListAsStringMeasure("key5", List.of("", "", ""));
    sensorTelemetry.report();

    verify(context).addTelemetryProperty("text.key1", "[\"\", \"value1\", \"value2\"]");
    verify(context).addTelemetryProperty("text.key2", "[\"value1\", \"\", \"value2\"]");
    verify(context).addTelemetryProperty("text.key3", "[\"value1\", \"value2\", \"\"]");
    verify(context).addTelemetryProperty("text.key4", "[\"value1\", \"value2\", \"value3\"]");
    verify(context).addTelemetryProperty("text.key5", "[\"\", \"\", \"\"]");
  }

  static List<SonarRuntime> provideTelemetrySonarRuntime() {
    return List.of(SONARQUBE_RUNTIME, SONARCLOUD_RUNTIME, SONARLINT_RUNTIME);
  }

  @Test
  void shouldNotReportNumericTelemetryWhenValueIsNegative() {
    var context = spy(SensorContextTester.create(Path.of(".")).setRuntime(SONARQUBE_RUNTIME));
    var sensorTelemetry = new TelemetryReporter(context);
    sensorTelemetry.addNumericMeasure("key1", -2);
    sensorTelemetry.addNumericMeasure("key2", 1);
    sensorTelemetry.addNumericMeasure("key2", -1);
    sensorTelemetry.report();

    verify(context, never()).addTelemetryProperty(eq("text.key1"), any());
    verify(context).addTelemetryProperty("text.key2", "1");
  }

  @Test
  void shouldNotReportStringTelemetryWhenValueIsTooLongOrShort() {
    var context = spy(SensorContextTester.create(Path.of(".")).setRuntime(SONARQUBE_RUNTIME));
    var sensorTelemetry = new TelemetryReporter(context);
    StringBuilder longTextBuilder = new StringBuilder();
    longTextBuilder.append("text".repeat(5000));
    sensorTelemetry.addStringMeasure("key1", longTextBuilder.toString());
    sensorTelemetry.addStringMeasure("key2", "txt");
    sensorTelemetry.addStringMeasure("key3", "exe");
    sensorTelemetry.addStringMeasure("key3", longTextBuilder.toString());
    sensorTelemetry.addStringMeasure("key4", "");
    sensorTelemetry.addStringMeasure("key5", "txt");
    sensorTelemetry.addStringMeasure("key5", "");
    sensorTelemetry.report();

    verify(context, never()).addTelemetryProperty(eq("text.key1"), any());
    verify(context).addTelemetryProperty("text.key2", "txt");
    verify(context).addTelemetryProperty("text.key3", "exe");
    verify(context).addTelemetryProperty("text.key5", "txt");
    assertThat(logTester.logs()).contains("Failed to add telemetry with key key1, value empty or bigger then specified string limit.");
    assertThat(logTester.logs()).contains("Failed to add telemetry with key key3, value empty or bigger then specified string limit.");
    assertThat(logTester.logs()).contains("Failed to add telemetry with key key4, value empty or bigger then specified string limit.");
    assertThat(logTester.logs()).contains("Failed to add telemetry with key key5, value empty or bigger then specified string limit.");
  }

  @Test
  void shouldNotReportListAsStringTelemetryWhenListIsEmpty() {
    var context = spy(SensorContextTester.create(Path.of(".")).setRuntime(SONARQUBE_RUNTIME));
    var sensorTelemetry = new TelemetryReporter(context);
    sensorTelemetry.addListAsStringMeasure("key1", new ArrayList<>());
    sensorTelemetry.report();

    verify(context, never()).addTelemetryProperty(eq("text.key1"), any());
  }

  @Test
  void shouldNotReportTelemetryWhenTelemetryNotSupported() {
    var context = spy(SensorContextTester.create(Path.of(".")).setRuntime(SONARQUBE_RUNTIME_WITHOUT_TELEMETRY_SUPPORT));
    var sensorTelemetry = new TelemetryReporter(context);
    sensorTelemetry.addNumericMeasure("key1", 5);
    sensorTelemetry.addStringMeasure("key2", "value");
    sensorTelemetry.report();

    verify(context, never()).addTelemetryProperty(any(), any());
  }

  @Test
  void shouldLogTelemetryWhenDurationStatisticsEnabled() {
    var context = spy(SensorContextTester.create(Path.of(".")).setRuntime(SONARQUBE_RUNTIME));
    context.settings().setProperty("sonar.text.duration.statistics", "true");
    var sensorTelemetry = new TelemetryReporter(context);
    sensorTelemetry.addStringMeasure("key1", "value");
    sensorTelemetry.addNumericMeasure("key2", 2);
    sensorTelemetry.report();

    logTester.logs();
    assertThat(logTester.logs())
      .contains("Reporting telemetry: text.key1=value")
      .contains("Reporting telemetry: text.key2=2");
  }
}
