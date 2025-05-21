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

import com.sun.management.OperatingSystemMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.config.Configuration;

/**
 * This class is used to monitor the java memory usage of the Text and Secrets analyzers.
 * It logs the overall statistics at the end to give a rough idea of the magnitude and proportion of memory usage.
 * All logs are in debug level.
 */
public final class MemoryMonitor {

  private static final Logger LOG = LoggerFactory.getLogger(MemoryMonitor.class);
  private static final String PROPERTY_KEY = "sonar.text.duration.statistics";
  private final List<MemoryRecord> memoryRecords = new ArrayList<>();
  private final boolean recordingEnabled;

  public MemoryMonitor(Configuration config) {
    recordingEnabled = config.getBoolean(PROPERTY_KEY).orElse(false);
    resetPeak();
    addRecord("Initial memory");
  }

  public void addRecord(String name) {
    if (recordingEnabled) {
      memoryRecords.add(new MemoryRecord(name, getMemoryUsed(), getPeakMemoryUsed()));
      resetPeak();
    }
  }

  public void logMemory() {
    if (!recordingEnabled) {
      return;
    }
    var sb = new StringBuilder();
    sb.append("Text and Secrets memory statistics (used, peak):");
    sb.append(System.lineSeparator());

    for (MemoryRecord memoryRecord : memoryRecords) {
      sb.append(memoryRecord)
        .append(System.lineSeparator());
    }
    sb.append("Note that these values may not be accurate due to garbage collection; they should only be used to detect significant outliers.");
    LOG.info(sb.toString());
    logAvailableMemory(ManagementFactory.getOperatingSystemMXBean());
  }

  private static long toMB(long value) {
    // Integral division because we don't need to track fractions of MB
    return value / (1024 * 1024);
  }

  private static long getAvailableSystemMemory(java.lang.management.OperatingSystemMXBean operatingSystemMXBean) throws NoSuchMethodError, ClassCastException {
    OperatingSystemMXBean os = (OperatingSystemMXBean) operatingSystemMXBean;
    return os.getTotalMemorySize();
  }

  private static long getAvailableRuntimeMemory() {
    return Runtime.getRuntime().maxMemory();
  }

  void logAvailableMemory(java.lang.management.OperatingSystemMXBean operatingSystemMXBean) {
    if (!recordingEnabled) {
      return;
    }
    try {
      long runtimeMemory = getAvailableRuntimeMemory();
      String runtimeMemoryInMB;
      if (runtimeMemory == Long.MAX_VALUE) {
        runtimeMemoryInMB = "unlimited";
      } else {
        runtimeMemoryInMB = toMB(runtimeMemory) + "MB";
      }
      LOG.info("Total system memory: {}, available runtime memory: {}", toMB(getAvailableSystemMemory(operatingSystemMXBean)) + "MB", runtimeMemoryInMB);
    } catch (ClassCastException | NoSuchMethodError e) {
      LOG.info("Could not get total system memory: {}", e.getMessage());
    }
  }

  private static long getMemoryUsed() {
    return ManagementFactory.getMemoryPoolMXBeans().stream()
      .filter(p -> p.getType() == MemoryType.HEAP)
      .mapToLong(p -> p.getUsage().getUsed())
      .sum();
  }

  private static long getPeakMemoryUsed() {
    return ManagementFactory.getMemoryPoolMXBeans().stream()
      .filter(p -> p.getType() == MemoryType.HEAP)
      .mapToLong(p -> p.getPeakUsage().getUsed())
      .sum();
  }

  private static void resetPeak() {
    ManagementFactory.getMemoryPoolMXBeans().stream()
      .filter(p -> p.getType() == MemoryType.HEAP)
      .forEach(MemoryPoolMXBean::resetPeakUsage);
  }

  private record MemoryRecord(String name, long used, long peak) {
    @Override
    public String toString() {
      return name + ": " + toMB(used) + "MB, " + toMB(peak) + "MB";
    }
  }
}
