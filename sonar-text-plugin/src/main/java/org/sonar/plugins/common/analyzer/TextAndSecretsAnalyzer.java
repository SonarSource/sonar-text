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
package org.sonar.plugins.common.analyzer;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.plugins.common.Check;
import org.sonar.plugins.common.InputFileContext;
import org.sonar.plugins.common.NotBinaryFilePredicate;
import org.sonar.plugins.common.TextAndSecretsSensor;
import org.sonar.plugins.common.measures.DurationStatistics;
import org.sonar.plugins.common.measures.MemoryMonitor;
import org.sonar.plugins.common.measures.TelemetryReporter;
import org.sonar.plugins.common.thread.ParallelizationManager;
import org.sonar.plugins.secrets.AbstractBinaryFileCheck;
import org.sonar.plugins.secrets.utils.CheckContainer;

public final class TextAndSecretsAnalyzer extends Analyzer {
  private static final Logger LOG = LoggerFactory.getLogger(TextAndSecretsAnalyzer.class);
  private static final String ANALYSIS_NAME = "text and secrets analysis";

  private final CheckContainer checkContainer;
  private final Map<String, Integer> extensionsFrequencyMap;

  public TextAndSecretsAnalyzer(
    SensorContext sensorContext,
    ParallelizationManager parallelizationManager,
    DurationStatistics durationStatistics,
    List<Check> suitableChecks,
    TelemetryReporter telemetryReporter,
    MemoryMonitor memoryMonitor,
    CheckContainer checkContainer) {
    super(sensorContext, parallelizationManager, durationStatistics, suitableChecks, ANALYSIS_NAME, telemetryReporter, memoryMonitor);
    this.checkContainer = checkContainer;
    this.extensionsFrequencyMap = new ConcurrentHashMap<>();
  }

  @Override
  protected boolean shouldAnalyzeFile(InputFileContext inputFileContext) {
    return containsNoBinaryCharacters(inputFileContext);
  }

  @Override
  protected void analyzeAllChecks(InputFileContext inputFileContext) {
    // Currently not possible and desired to parallelize check execution per file, as we rely on the sequential and always same order of the
    // checks to achieve deterministic analysis results
    // The main reason is because of the calculation of overlapping reported secrets in InputFileContext
    try {
      checkContainer.analyze(inputFileContext);
    } catch (RuntimeException e) {
      logAnalysisError(inputFileContext.getInputFile(), e);
    }
  }

  /**
   * We suppose here that all provided files have been whitelisted, so we don't expected binary files.
   * In case it still happen, we don't add the extension to the blacklist as we consider it to be an exception.
   */
  private boolean containsNoBinaryCharacters(InputFileContext inputFileContext) {
    boolean hasNonTextCharacters = inputFileContext.hasNonTextCharacters();
    if (hasNonTextCharacters) {
      String fileExtension = NotBinaryFilePredicate.extension(inputFileContext.getInputFile().filename());
      extensionsFrequencyMap.merge(fileExtension != null ? fileExtension : "", 1, Integer::sum);
      LOG.warn("The file '{}' contains binary data and will not be included in the text and secrets analysis.", inputFileContext.getInputFile());
      if (inputFileContext.getInputFile().language() != null) {
        LOG.warn("Please check this file and/or exclude it from the analysis with sonar.exclusions property.");
      } else {
        LOG.warn("Please check this file and/or remove the extension from the '{}' property.", TextAndSecretsSensor.TEXT_INCLUSIONS_KEY);
      }
    }
    return !hasNonTextCharacters;
  }

  public static List<Check> filterSuitableChecks(Collection<Check> checks) {
    return checks.stream()
      .filter(check -> !(check instanceof AbstractBinaryFileCheck))
      .toList();
  }

  @Override
  protected void processFileTelemetryMeasures() {
    super.processFileTelemetryMeasures();
    reportExcludedBinaryExtensions(extensionsFrequencyMap, telemetryReporter);
  }

  static void reportExcludedBinaryExtensions(Map<String, Integer> extensionsFrequencyMap, TelemetryReporter telemetryReporter) {
    if (!extensionsFrequencyMap.isEmpty()) {
      telemetryReporter.addListAsStringMeasure("excluded.binary.extensions", getFrequentKeys(extensionsFrequencyMap, 1000));
    }
  }

  static List<String> getFrequentKeys(Map<String, Integer> frequencyMap, int limit) {
    if (limit < 0) {
      return Collections.emptyList();
    }
    return frequencyMap.entrySet().stream()
      .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
      .map(Map.Entry::getKey)
      .limit(limit)
      .toList();
  }
}
