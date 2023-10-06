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

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonarsource.analyzer.commons.ProgressReport;

public final class Analyzer {
  private static boolean displayHelpAboutExcludingBinaryFile = true;
  private static final int REPORT_REFRESH_TIME_IN_SECONDS = 10;
  private static final Logger LOG = LoggerFactory.getLogger(Analyzer.class);

  private Analyzer() {
  }

  public static void analyzeFiles(SensorContext sensorContext, List<Check> activeChecks, NotBinaryFilePredicate notBinaryFilePredicate, boolean analyzeAllFilesMode,
    Collection<InputFile> inputFiles) {
    displayHelpAboutExcludingBinaryFile = true;
    List<String> filenames = inputFiles.stream().map(InputFile::toString).collect(Collectors.toList());
    var progressReport = new ProgressReport("Progress of the text and secrets analysis", TimeUnit.SECONDS.toMillis(REPORT_REFRESH_TIME_IN_SECONDS));
    progressReport.start(filenames);
    var cancelled = false;

    try {
      for (InputFile inputFile : inputFiles) {
        if (sensorContext.isCancelled()) {
          cancelled = true;
          break;
        }

        analyzeFile(sensorContext, inputFile, analyzeAllFilesMode, notBinaryFilePredicate, activeChecks);
        progressReport.nextFile();
      }
    } finally {
      if (cancelled) {
        progressReport.cancel();
      } else {
        progressReport.stop();
      }
    }
  }

  private static void analyzeFile(SensorContext sensorContext, InputFile inputFile, boolean analyzeAllFilesMode, NotBinaryFilePredicate notBinaryFilePredicate,
    List<Check> activeChecks) {
    try {
      var inputFileContext = new InputFileContext(sensorContext, inputFile);
      if (analyzeAllFilesMode) {
        analyzeFilesInBlacklistMode(notBinaryFilePredicate, inputFileContext, activeChecks);
      } else {
        analyzeFilesInWhitelistMode(inputFileContext, activeChecks);
      }
    } catch (IOException | RuntimeException e) {
      logAnalysisError(sensorContext, inputFile, e);
    }
  }

  /**
   * We suppose here that the list of provided files may contain some binary files that we couldn't exclude beforehand.
   * If that happen, we add its extension dynamically in the blacklist to avoid all files with the same extension.
   */
  private static void analyzeFilesInBlacklistMode(NotBinaryFilePredicate notBinaryFilePredicate, InputFileContext inputFileContext, List<Check> activeChecks) {
    if (notBinaryFilePredicate.apply(inputFileContext.getInputFile())) {
      if (inputFileContext.hasNonTextCharacters()) {
        excludeBinaryFileExtension(notBinaryFilePredicate, inputFileContext.getInputFile());
      } else {
        analyzeAllChecks(inputFileContext, activeChecks);
      }
    }
  }

  /**
   * We suppose here that all provided files have been whitelisted, so we don't expected binary files.
   * In case it still happen, we don't add the extension to the blacklist as we consider it to be an exception.
   */
  private static void analyzeFilesInWhitelistMode(InputFileContext inputFileContext, List<Check> activeChecks) {
    if (inputFileContext.hasNonTextCharacters()) {
      LOG.warn("The file '{}' contains binary data and will not be analyzed.", inputFileContext.getInputFile().filename());
      LOG.warn("Please check this file and/or remove the extension from the 'sonar.text.included.file.suffixes' property.");
    } else {
      analyzeAllChecks(inputFileContext, activeChecks);
    }
  }

  private static void analyzeAllChecks(InputFileContext inputFileContext, List<Check> activeChecks) {
    for (Check check : activeChecks) {
      check.analyze(inputFileContext);
    }
  }

  private static void excludeBinaryFileExtension(NotBinaryFilePredicate notBinaryFilePredicate, InputFile inputFile) {
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

  private static void logAnalysisError(SensorContext sensorContext, InputFile inputFile, Exception e) {
    var message = String.format("Unable to analyze file %s: %s", inputFile, e.getMessage());
    sensorContext.newAnalysisError()
      .message(message)
      .onFile(inputFile)
      .save();
    LOG.warn(message);
    var exceptionString = e.toString();
    LOG.debug(exceptionString);
  }
}
