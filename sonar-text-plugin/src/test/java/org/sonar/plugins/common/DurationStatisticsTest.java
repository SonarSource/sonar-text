/*
 * SonarQube Text Plugin
 * Copyright (C) 2021-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.plugins.common;

import java.nio.file.Paths;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;

import static org.assertj.core.api.Assertions.assertThat;

class DurationStatisticsTest {
  @RegisterExtension
  LogTesterJUnit5 logTester = new LogTesterJUnit5();

  private static final Pattern REPORT_PATTERN = Pattern.compile(".*Statistics\\s+ {2}.*::.* \\d+ ms \\d+ times \\(mean [\\d']+ us\\)(.|\\s)*");

  @Test
  void shouldRecordSimpleStatistics() {
    var sensorContext = SensorContextTester.create(Paths.get("."));
    sensorContext.settings().setProperty("sonar.text.duration.statistics", "true");
    DurationStatistics durationStatistics = new DurationStatistics(sensorContext.config());
    durationStatistics.timed("test::total", () -> doNothingFor(100));
    durationStatistics.log();

    assertThat(logTester.logs()).hasSize(2);
    assertThat(logTester.logs().get(0)).matches(REPORT_PATTERN);
    assertThat(logTester.logs().get(1)).endsWith("Granular Duration Statistics" + System.lineSeparator());
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

    assertThat(logTester.logs()).hasSize(2);
    assertThat(logTester.logs().get(0)).matches(REPORT_PATTERN);
    assertThat(logTester.logs().get(1)).matches(REPORT_PATTERN);
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
