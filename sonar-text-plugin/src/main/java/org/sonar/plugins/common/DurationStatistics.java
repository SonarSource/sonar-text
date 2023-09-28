/*
 * SonarQube Text Plugin
 * Copyright (C) 2021-2023 SonarSource SA
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

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.Format;
import java.text.NumberFormat;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.config.Configuration;

public class DurationStatistics {
  private static final Logger LOGGER = LoggerFactory.getLogger(DurationStatistics.class);

  private static final String PROPERTY_KEY = "sonar.text.duration.statistics";

  // modifier is package-private to make visible in tests
  final Map<String, Measurement> stats = new ConcurrentHashMap<>();

  private final AtomicBoolean isRecordingEnabled = new AtomicBoolean(false);

  public DurationStatistics(Configuration config) {
    config.getBoolean(PROPERTY_KEY).ifPresent(isRecordingEnabled::set);
  }

  public <T> T timed(String id, Supplier<T> supplier) {
    if (isRecordingEnabled.get()) {
      long startTime = System.nanoTime();
      var result = supplier.get();
      addRecord(id, System.nanoTime() - startTime);
      return result;
    } else {
      return supplier.get();
    }
  }

  void addRecord(String id, long elapsedTime) {
    stats.computeIfAbsent(id, key -> new Measurement()).add(elapsedTime);
  }

  public void log() {
    if (isRecordingEnabled.get()) {
      var out = new StringBuilder();
      var symbols = new DecimalFormatSymbols(Locale.ROOT);
      symbols.setGroupingSeparator('\'');
      NumberFormat format = new DecimalFormat("#,###", symbols);
      out.append("Duration Statistics");
      out.append(formatEntries(format, stats.entrySet().stream().filter(s -> s.getKey().endsWith("-total"))));
      var generalReport = out.toString();
      LOGGER.info(generalReport);

      out = new StringBuilder();
      out.append("Granular Duration Statistics");
      out.append(formatEntries(format, stats.entrySet().stream().filter(s -> !s.getKey().endsWith("-total"))));
      var verboseReport = out.toString();
      LOGGER.info(verboseReport);
    }
  }

  private static StringBuilder formatEntries(Format format, Stream<Map.Entry<String, Measurement>> entries) {
    var sb = new StringBuilder();
    entries.sorted(Comparator.<Map.Entry<String, Measurement>>comparingLong(entry -> entry.getValue().total.get()).reversed())
      .forEach(e -> sb.append(formatEntry(format, e.getKey(), e.getValue())));
    return sb;
  }

  private static StringBuilder formatEntry(Format format, String id, Measurement measurement) {
    var totalMs = measurement.total.get() / 1_000_000L;
    var count = measurement.count.get();
    var meanMs = totalMs * 1.0 / count * 1_000;
    return new StringBuilder(", ")
      .append(id)
      .append(" ")
      .append(format.format(totalMs))
      .append(" ms ")
      .append(measurement.count.get())
      .append(" times (mean ")
      .append(format.format(meanMs))
      .append(" us)")
      .append(System.lineSeparator());
  }

  private static class Measurement {
    private final AtomicLong count = new AtomicLong();
    private final AtomicLong total = new AtomicLong();

    public void add(long delta) {
      count.incrementAndGet();
      total.addAndGet(delta);
    }
  }
}
