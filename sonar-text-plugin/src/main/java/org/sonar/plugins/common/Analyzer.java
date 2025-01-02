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

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.plugins.common.thread.ParallelizationManager;

public final class Analyzer {
  private static final Logger LOG = LoggerFactory.getLogger(Analyzer.class);
  private final SensorContext sensorContext;
  private final ParallelizationManager parallelizationManager;
  private final DurationStatistics durationStatistics;
  private final List<Check> activeChecks;
  private final NotBinaryFilePredicate notBinaryFilePredicate;
  private final boolean analyzeAllFilesMode;
  private boolean displayHelpAboutExcludingBinaryFile = true;

  public Analyzer(
    SensorContext sensorContext,
    ParallelizationManager parallelizationManager,
    DurationStatistics durationStatistics,
    List<Check> activeChecks,
    NotBinaryFilePredicate notBinaryFilePredicate,
    boolean analyzeAllFilesMode) {
    this.sensorContext = sensorContext;
    this.parallelizationManager = parallelizationManager;
    this.durationStatistics = durationStatistics;
    this.activeChecks = activeChecks;
    this.notBinaryFilePredicate = notBinaryFilePredicate;
    this.analyzeAllFilesMode = analyzeAllFilesMode;
  }

  public void analyzeFiles(List<InputFile> inputFiles) {
    displayHelpAboutExcludingBinaryFile = true;

    // We want to analyze the files with the highest number of lines first, in order to fully utilize threads
    inputFiles.sort(Comparator.comparingInt(inputFile -> -inputFile.lines()));

    List<InputFileContext> analyzableFiles = durationStatistics.timed("preparingInputFiles" + DurationStatistics.SUFFIX_GENERAL,
      () -> buildInputFileContexts(inputFiles)
        .filter(Objects::nonNull)
        .filter(this::shouldBeAnalyzed)
        .toList());

    if (analyzableFiles.isEmpty()) {
      LOG.debug("There are no files to be analyzed");
      return;
    }

    durationStatistics.timed("analyzingAllChecks" + DurationStatistics.SUFFIX_GENERAL, () -> analyzeAllFiles(analyzableFiles));
  }

  private void analyzeAllFiles(List<InputFileContext> analyzableFiles) {
    var cancelled = false;
    var progressReport = new MultiFileProgressReport();
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

  private boolean shouldBeAnalyzed(InputFileContext inputFileContext) {
    if (analyzeAllFilesMode) {
      return shouldBeAnalyzedBlacklistMode(inputFileContext);
    } else {
      return shouldBeAnalyzedWhitelistMode(inputFileContext);
    }
  }

  /**
   * We suppose here that the list of provided files may contain some binary files that we couldn't exclude beforehand.
   * If that happen, we add its extension dynamically in the blacklist to avoid all files with the same extension.
   */
  private boolean shouldBeAnalyzedBlacklistMode(InputFileContext inputFileContext) {
    if (notBinaryFilePredicate.apply(inputFileContext.getInputFile())) {
      boolean hasNonTextCharacters = inputFileContext.hasNonTextCharacters();
      if (hasNonTextCharacters) {
        excludeBinaryFileExtension(inputFileContext.getInputFile());
      }
      return !hasNonTextCharacters;
    }
    return false;
  }

  /**
   * We suppose here that all provided files have been whitelisted, so we don't expected binary files.
   * In case it still happen, we don't add the extension to the blacklist as we consider it to be an exception.
   */
  private static boolean shouldBeAnalyzedWhitelistMode(InputFileContext inputFileContext) {
    boolean hasNonTextCharacters = inputFileContext.hasNonTextCharacters();
    if (hasNonTextCharacters) {
      LOG.warn("The file '{}' contains binary data and will not be analyzed.", inputFileContext.getInputFile());
      LOG.warn("Please check this file and/or remove the extension from the '{}' property.", TextAndSecretsSensor.TEXT_INCLUSIONS_KEY);
    }
    return !hasNonTextCharacters;
  }

  private void analyzeAllChecks(InputFileContext inputFileContext) {
    // Currently not possible and desired to parallelize, as we rely on the sequential and always same order of the checks to achieve
    // deterministic analysis results
    // The main reason is because of the calculation of overlapping reported secrets in InputFileContext
    try {
      for (Check check : activeChecks) {
        check.analyze(inputFileContext);
      }
    } catch (RuntimeException e) {
      logAnalysisError(inputFileContext.getInputFile(), e);
    }
  }

  private void excludeBinaryFileExtension(InputFile inputFile) {
    String extension = NotBinaryFilePredicate.extension(inputFile.filename());
    if (extension != null) {
      notBinaryFilePredicate.addBinaryFileExtension(extension);
      LOG.warn("'{}' was added to the binary file filter because the file '{}' is a binary file.", extension, inputFile);
      if (displayHelpAboutExcludingBinaryFile) {
        displayHelpAboutExcludingBinaryFile = false;
        LOG.info("To remove the previous warning you can add the '.{}' extension to the '{}' property.", extension,
          TextAndSecretsSensor.EXCLUDED_FILE_SUFFIXES_KEY);
      }
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
