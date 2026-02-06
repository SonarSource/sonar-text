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

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.CheckForNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.utils.Version;
import org.sonar.plugins.common.Check;
import org.sonar.plugins.common.InputFileContext;
import org.sonar.plugins.common.MultiFileProgressReport;
import org.sonar.plugins.common.measures.DurationStatistics;
import org.sonar.plugins.common.measures.MemoryMonitor;
import org.sonar.plugins.common.measures.TelemetryReporter;
import org.sonar.plugins.common.thread.ParallelizationManager;

public class Analyzer {
  public static final Version HIDDEN_FILES_SUPPORTED_API_VERSION = Version.create(12, 0);
  public static final String ANALYZED_FILES_MEASURE_KEY = "analyzed_files_count";
  public static final String ANALYZED_HIDDEN_FILES_MEASURE_KEY = "analyzed_hidden_files_count";
  private static final Logger LOG = LoggerFactory.getLogger(Analyzer.class);

  private final SensorContext sensorContext;
  private final ParallelizationManager parallelizationManager;
  protected final DurationStatistics durationStatistics;
  private final List<Check> suitableChecks;
  private final String analysisName;
  protected final TelemetryReporter telemetryReporter;
  private final MemoryMonitor memoryMonitor;
  private final boolean supportedHiddenFileAnalysis;
  private final AtomicInteger analyzedFiles = new AtomicInteger(0);
  private final AtomicInteger analyzedHiddenFiles = new AtomicInteger(0);

  protected Analyzer(
    SensorContext sensorContext,
    ParallelizationManager parallelizationManager,
    DurationStatistics durationStatistics,
    List<Check> suitableChecks,
    String analysisName,
    TelemetryReporter telemetryReporter,
    MemoryMonitor memoryMonitor) {
    this.sensorContext = sensorContext;
    this.parallelizationManager = parallelizationManager;
    this.durationStatistics = durationStatistics;
    this.suitableChecks = suitableChecks;
    this.analysisName = analysisName;
    this.telemetryReporter = telemetryReporter;
    this.memoryMonitor = memoryMonitor;
    this.supportedHiddenFileAnalysis = sensorContext.runtime().getApiVersion().isGreaterThanOrEqual(HIDDEN_FILES_SUPPORTED_API_VERSION);
  }

  public void analyzeFiles(List<InputFile> inputFiles) {
    if (inputFiles.isEmpty()) {
      LOG.info("There are no files to be analyzed for the {}", analysisName);
      return;
    }

    LOG.info("Starting the {}", analysisName);

    analyzeAllFiles(inputFiles);
    memoryMonitor.addRecord("After the " + analysisName);
  }

  /**
   * Analyzers can override this method to filter the {@link InputFileContext} they want to analyze.
   * By default, no filtering is done.
   */
  protected boolean shouldAnalyzeFile(InputFileContext inputFileContext) {
    return true;
  }

  private void analyzeAllFiles(List<InputFile> inputFiles) {
    var cancelled = false;
    var progressReport = new MultiFileProgressReport(analysisName);
    progressReport.start(inputFiles.size());

    try {
      for (InputFile inputFile : inputFiles) {
        if (sensorContext.isCancelled()) {
          cancelled = true;
          break;
        }
        parallelizationManager.submit(() -> {
          progressReport.startAnalysisFor(inputFile.toString());
          prepareAndAnalyze(inputFile);
          progressReport.finishAnalysisFor(inputFile.toString());
        });
      }
      parallelizationManager.drainThreads();
    } finally {
      if (cancelled) {
        progressReport.cancel();
      } else {
        progressReport.stop();
      }
    }
    processFileTelemetryMeasures();
  }

  private void prepareAndAnalyze(InputFile inputFile) {
    triggerMetadataGeneration(inputFile);
    var inputFileContext = durationStatistics.timed("preparingInputFiles" + DurationStatistics.SUFFIX_GENERAL, () -> buildInputFileContext(inputFile));

    if (inputFileContext != null && shouldAnalyzeFile(inputFileContext)) {
      durationStatistics.timed("analyzingAllChecks" + DurationStatistics.SUFFIX_GENERAL, () -> analyzeAllChecks(inputFileContext));
      countAnalyzedFile(inputFile);
    }
  }

  private static synchronized void triggerMetadataGeneration(InputFile inputFile) {
    // Triggers metadata-generation for every inputFile synchronized
    // parallel generation is currently not supported / not guaranteed to work
    inputFile.lines();
  }

  @CheckForNull
  private InputFileContext buildInputFileContext(InputFile inputFile) {
    try {
      return new InputFileContext(sensorContext, inputFile);
    } catch (IOException | RuntimeException e) {
      logAnalysisError(inputFile, e);
      return null;
    }
  }

  protected void analyzeAllChecks(InputFileContext inputFileContext) {
    // Currently not possible and desired to parallelize check execution per file, as we rely on the sequential and always same order of the
    // checks to achieve deterministic analysis results
    // The main reason is because of the calculation of overlapping reported secrets in InputFileContext
    try {
      for (Check check : suitableChecks) {
        check.analyze(inputFileContext);
      }
      inputFileContext.flushIssues();
    } catch (RuntimeException e) {
      logAnalysisError(inputFileContext.getInputFile(), e);
    }
  }

  protected void logAnalysisError(InputFile inputFile, Exception e) {
    var message = String.format("Unable to analyze file %s: %s", inputFile, e.getMessage());
    sensorContext.newAnalysisError()
      .message(message)
      .onFile(inputFile)
      .save();
    LOG.warn(message);
  }

  private void countAnalyzedFile(InputFile inputFile) {
    analyzedFiles.incrementAndGet();
    if (supportedHiddenFileAnalysis && inputFile.isHidden()) {
      analyzedHiddenFiles.incrementAndGet();
    }
  }

  protected void processFileTelemetryMeasures() {
    int numberOfAnalyzedFiles = this.analyzedFiles.get();
    LOG.debug("Analyzed files for the {}: {}", analysisName, numberOfAnalyzedFiles);
    telemetryReporter.addNumericMeasure(ANALYZED_FILES_MEASURE_KEY, numberOfAnalyzedFiles);

    if (supportedHiddenFileAnalysis) {
      telemetryReporter.addNumericMeasure(ANALYZED_HIDDEN_FILES_MEASURE_KEY, analyzedHiddenFiles.get());
    }
  }
}
