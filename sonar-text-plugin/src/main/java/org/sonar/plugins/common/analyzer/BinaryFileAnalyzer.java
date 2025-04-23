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
package org.sonar.plugins.common.analyzer;

import java.util.Collection;
import java.util.List;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.plugins.common.Check;
import org.sonar.plugins.common.DurationStatistics;
import org.sonar.plugins.common.thread.ParallelizationManager;
import org.sonar.plugins.secrets.BinaryFileCheck;

public final class BinaryFileAnalyzer extends Analyzer {
  private static final String ANALYSIS_NAME = "binary file analysis";

  public BinaryFileAnalyzer(
    SensorContext sensorContext,
    ParallelizationManager parallelizationManager,
    DurationStatistics durationStatistics,
    List<Check> suitableChecks) {
    super(sensorContext, parallelizationManager, durationStatistics, suitableChecks, ANALYSIS_NAME);
  }

  @Override
  protected void sortInputFiles(List<InputFile> inputFiles) {
    // we don't need to sort binary files as they don't have valid line numbers
  }

  public static List<Check> filterSuitableChecks(Collection<Check> checks) {
    return checks.stream()
      .filter(BinaryFileCheck.class::isInstance)
      .toList();
  }
}
