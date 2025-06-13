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
package org.sonar.plugins.common.measures;

import java.nio.file.Paths;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.event.Level;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;

import static org.assertj.core.api.Assertions.assertThat;

class DurationStatisticsTest {
  @RegisterExtension
  LogTesterJUnit5 logTester = new LogTesterJUnit5().setLevel(Level.DEBUG);

  private static final Pattern REPORT_PATTERN = Pattern.compile(".*Statistics\\s+ {2}.*::.* \\d+ ms \\d+ times \\(mean [\\d']+ us\\)(.|\\s)*");

  @ParameterizedTest
  @ValueSource(strings = {"true", "false"})
  void shouldEnableCorrectlyBasedOnConfig(boolean isEnabled) {
    var sensorContext = SensorContextTester.create(Paths.get("."));
    sensorContext.settings().setProperty("sonar.text.duration.statistics", isEnabled);
    DurationStatistics durationStatistics = new DurationStatistics(sensorContext.config());

    assertThat(durationStatistics.isRecordingEnabled()).isEqualTo(isEnabled);
  }

  @Test
  void shouldRecordSimpleStatistics() {
    var sensorContext = SensorContextTester.create(Paths.get("."));
    sensorContext.settings().setProperty("sonar.text.duration.statistics", "true");
    DurationStatistics durationStatistics = new DurationStatistics(sensorContext.config());
    durationStatistics.timed("analyze::general", () -> doNothingFor(100));
    durationStatistics.timed("test::total", () -> doNothingFor(100));
    durationStatistics.log();

    assertThat(logTester.logs()).hasSize(3);
    assertThat(logTester.logs().get(1)).matches(REPORT_PATTERN);
    assertThat(logTester.logs().get(2)).endsWith("Granular Secret Matcher Duration Statistics" + System.lineSeparator());
  }

  @Test
  void shouldRecordMultipleStatistics() {
    var sensorContext = SensorContextTester.create(Paths.get("."));
    sensorContext.settings().setProperty("sonar.text.duration.statistics", "true");
    DurationStatistics durationStatistics = new DurationStatistics(sensorContext.config());
    durationStatistics.timed("test-1::total", () -> doNothingFor(100));
    durationStatistics.timed("test-1::total", () -> doNothingFor(50));
    durationStatistics.timed("test-2::total", () -> doNothingFor(100));
    durationStatistics.timed("test-2::part1", () -> doNothingFor(10));
    durationStatistics.timed("test-2::part2", () -> doNothingFor(20));
    durationStatistics.log();

    assertThat(logTester.logs()).hasSize(3);
    assertThat(logTester.logs().get(1)).matches(REPORT_PATTERN);
    assertThat(logTester.logs().get(2)).matches(REPORT_PATTERN);
  }

  @Test
  void shouldRecordMultipleStatisticsWithAggregatedTotals() {
    var sensorContext = SensorContextTester.create(Paths.get("."));
    sensorContext.settings().setProperty("sonar.text.duration.statistics", "true");
    DurationStatistics durationStatistics = new DurationStatistics(sensorContext.config());
    durationStatistics.timed("test-1::total", () -> doNothingFor(100));
    durationStatistics.timed("test-1::preFilter", () -> doNothingFor(50));
    durationStatistics.timed("test-2::preFilter", () -> doNothingFor(30));
    durationStatistics.timed("test-1::matcher", () -> doNothingFor(20));
    durationStatistics.timed("test-2::matcher", () -> doNothingFor(40));
    durationStatistics.timed("test-1::postFilter", () -> doNothingFor(10));
    durationStatistics.timed("test-2::postFilter", () -> doNothingFor(20));
    durationStatistics.timed("test-3::postFilter", () -> doNothingFor(10));
    durationStatistics.log();

    assertThat(logTester.logs()).hasSize(3);
    assertThat(logTester.logs().get(0)).matches(REPORT_PATTERN);
    assertThat(logTester.logs().get(0)).contains("preFilter::general", "postFilter::general", "matcher::general");
    assertThat(logTester.logs().get(1)).matches(REPORT_PATTERN);
    assertThat(logTester.logs().get(2)).matches(REPORT_PATTERN);
  }

  @Test
  void shouldOnlyLogGeneralInInfoLogLevel() {
    logTester.setLevel(Level.INFO);
    var sensorContext = SensorContextTester.create(Paths.get("."));
    sensorContext.settings().setProperty("sonar.text.duration.statistics", "true");
    DurationStatistics durationStatistics = new DurationStatistics(sensorContext.config());
    durationStatistics.timed("test-1::total", () -> doNothingFor(100));
    durationStatistics.timed("test-1::preFilter", () -> doNothingFor(50));
    durationStatistics.timed("test-2::preFilter", () -> doNothingFor(30));
    durationStatistics.timed("test-1::matcher", () -> doNothingFor(20));
    durationStatistics.timed("test-2::matcher", () -> doNothingFor(40));
    durationStatistics.timed("test-1::postFilter", () -> doNothingFor(10));
    durationStatistics.timed("test-2::postFilter", () -> doNothingFor(20));
    durationStatistics.timed("test-3::postFilter", () -> doNothingFor(10));
    durationStatistics.log();

    assertThat(logTester.logs()).hasSize(1);
    assertThat(logTester.logs().get(0)).matches(REPORT_PATTERN);
    assertThat(logTester.logs().get(0)).contains("preFilter::general", "postFilter::general", "matcher::general");
  }

  private static Object doNothingFor(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    return null;
  }
}
