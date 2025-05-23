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

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.IndexedFile;
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
  private final DurationStatistics durationStatistics;
  private final List<Check> suitableChecks;
  private final String analysisName;
  private final TelemetryReporter telemetryReporter;
  private final MemoryMonitor memoryMonitor;

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
  }

  public void analyzeFiles(List<InputFile> inputFiles) {
    sortInputFiles(inputFiles);

    List<InputFileContext> analyzableFiles = durationStatistics.timed("preparingInputFiles" + DurationStatistics.SUFFIX_GENERAL,
      () -> buildInputFileContexts(inputFiles)
        .filter(Objects::nonNull)
        .filter(this::shouldAnalyzeFile)
        .toList());

    memoryMonitor.addRecord("After preparation of input files for the " + analysisName);

    if (analyzableFiles.isEmpty()) {
      LOG.info("There are no files to be analyzed for the {}", analysisName);
      return;
    }
    LOG.info("Starting the {}", analysisName);

    durationStatistics.timed("analyzingAllChecks" + DurationStatistics.SUFFIX_GENERAL, () -> analyzeAllFiles(analyzableFiles));
    memoryMonitor.addRecord("After the " + analysisName);
  }

  /**
   * In order to fully utilize threads we want to analyze the files with the highest number of lines first
   */
  protected void sortInputFiles(List<InputFile> inputFiles) {
    inputFiles.sort(Comparator.comparingInt(inputFile -> -inputFile.lines()));
  }

  /**
   * Analyzers can override this method to filter the {@link InputFileContext} they want to analyze.
   * By default, no filtering is done.
   */
  protected boolean shouldAnalyzeFile(InputFileContext inputFileContext) {
    return true;
  }

  private void analyzeAllFiles(List<InputFileContext> analyzableFiles) {
    var cancelled = false;
    var progressReport = new MultiFileProgressReport(analysisName);
    progressReport.start(analyzableFiles.size());
    try {
      for (InputFileContext inputFileContext : analyzableFiles) {
        if (sensorContext.isCancelled()) {
          cancelled = true;
          break;
        }
        parallelizationManager.submit(() -> {
          progressReport.startAnalysisFor(inputFileContext.getInputFile().toString());
          analyzeAllChecks(inputFileContext);
          progressReport.finishAnalysisFor(inputFileContext.getInputFile().toString());
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
    processFileTelemetryMeasures(analyzableFiles);
  }

  private void processFileTelemetryMeasures(List<InputFileContext> analyzedFiles) {
    telemetryReporter.addNumericMeasure(ANALYZED_FILES_MEASURE_KEY, analyzedFiles.size());
    var runtimeVersion = sensorContext.runtime().getApiVersion();
    if (runtimeVersion.isGreaterThanOrEqual(HIDDEN_FILES_SUPPORTED_API_VERSION)) {
      var hiddenFilesCount = analyzedFiles.stream().map(InputFileContext::getInputFile).filter(IndexedFile::isHidden).count();
      telemetryReporter.addNumericMeasure(ANALYZED_HIDDEN_FILES_MEASURE_KEY, (int) hiddenFilesCount);
    }
  }

  private Stream<InputFileContext> buildInputFileContexts(List<InputFile> inputFiles) {
    var analyzableFiles = new InputFileContext[inputFiles.size()];
    var currentFileNumber = 0;
    for (InputFile inputFile : inputFiles) {
      // preserving the initial order of the files
      var index = currentFileNumber;
      currentFileNumber++;
      parallelizationManager.submit(() -> {
        try {
          var inputFileContext = new InputFileContext(sensorContext, inputFile);
          // will not create race conditions, as every thread is working on a different index
          analyzableFiles[index] = inputFileContext;
        } catch (IOException | RuntimeException e) {
          logAnalysisError(inputFile, e);
        }
      });
    }
    parallelizationManager.drainThreads();
    return Stream.of(analyzableFiles);
  }

  private void analyzeAllChecks(InputFileContext inputFileContext) {
    // Currently not possible and desired to parallelize, as we rely on the sequential and always same order of the checks to achieve
    // deterministic analysis results
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

  private void logAnalysisError(InputFile inputFile, Exception e) {
    var message = String.format("Unable to analyze file %s: %s", inputFile, e.getMessage());
    sensorContext.newAnalysisError()
      .message(message)
      .onFile(inputFile)
      .save();
    LOG.warn(message);
    // TODO SECRETS-114: remove print of stacktrace
    LOG.debug("{}: ", e, e);
  }
}
