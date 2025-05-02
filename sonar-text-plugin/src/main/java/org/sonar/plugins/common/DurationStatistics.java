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

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.Format;
import java.text.NumberFormat;
import java.util.Collections;
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

  public static final String SUFFIX_GENERAL = "::general";
  public static final String SUFFIX_TOTAL = "::total";
  public static final String SUFFIX_PRE = "::preFilter";
  public static final String SUFFIX_MATCHER = "::matcher";
  public static final String SUFFIX_POST = "::postFilter";

  private final Map<String, Measurement> stats = new ConcurrentHashMap<>();

  private final AtomicBoolean isRecordingEnabled = new AtomicBoolean(false);

  private final NumberFormat format;

  public DurationStatistics(Configuration config) {
    config.getBoolean(PROPERTY_KEY).ifPresent(isRecordingEnabled::set);

    var symbols = new DecimalFormatSymbols(Locale.ROOT);
    symbols.setGroupingSeparator('\'');
    this.format = new DecimalFormat("#,###", symbols);
  }

  public boolean isRecordingEnabled() {
    return isRecordingEnabled.get();
  }

  public void timed(String id, Runnable runnable) {
    timed(id, () -> {
      runnable.run();
      return null;
    });
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
      calculateSecretMatcherTotals();

      var sbGeneral = new StringBuilder("Duration Statistics")
        .append(System.lineSeparator())
        .append(formatEntries(format, stats.entrySet().stream().filter(s -> s.getKey().endsWith(SUFFIX_GENERAL))));
      LOGGER.info("{}", sbGeneral);

      var sbMatcher = new StringBuilder("Secret Matcher Duration Statistics")
        .append(System.lineSeparator())
        .append(formatEntries(format, stats.entrySet().stream().filter(s -> s.getKey().endsWith(SUFFIX_TOTAL))));
      LOGGER.info("{}", sbMatcher);

      var sbMatcherVerbose = new StringBuilder("Granular Secret Matcher Duration Statistics")
        .append(System.lineSeparator())
        .append(formatEntries(format,
          stats.entrySet().stream().filter(s -> !s.getKey().endsWith(SUFFIX_TOTAL) && !s.getKey().endsWith(SUFFIX_GENERAL))));
      LOGGER.info("{}", sbMatcherVerbose);
    }
  }

  private void calculateSecretMatcherTotals() {
    for (Map.Entry<String, Measurement> entry : Collections.unmodifiableSet(stats.entrySet())) {
      if (entry.getKey().endsWith(SUFFIX_PRE)) {
        addRecord("preFilter" + SUFFIX_TOTAL, entry.getValue().total.get());
      } else if (entry.getKey().endsWith(SUFFIX_MATCHER)) {
        addRecord("matcher" + SUFFIX_TOTAL, entry.getValue().total.get());
      } else if (entry.getKey().endsWith(SUFFIX_POST)) {
        addRecord("postFilter" + SUFFIX_TOTAL, entry.getValue().total.get());
      }
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
    return new StringBuilder("  ")
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
