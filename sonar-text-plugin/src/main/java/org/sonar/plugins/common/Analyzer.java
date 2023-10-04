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

public class Analyzer {
  private static final int REPORT_REFRESH_TIME_IN_SECONDS = 10;
  private static final Logger LOG = LoggerFactory.getLogger(Analyzer.class);
  private boolean displayHelpAboutExcludingBinaryFile = true;

  private SensorContext sensorContext;
  private List<Check> activeChecks;
  private NotBinaryFilePredicate notBinaryFilePredicate;
  private boolean analyzeAllFilesMode;

  public Analyzer() {
    // Empty analyzer by default, configuration provided by setters
  }

  public Analyzer sensorContext(SensorContext sensorContext) {
    this.sensorContext = sensorContext;
    return this;
  }

  public Analyzer activeChecks(List<Check> activeChecks) {
    this.activeChecks = activeChecks;
    return this;
  }

  public Analyzer notBinaryFilePredicate(NotBinaryFilePredicate notBinaryFilePredicate) {
    this.notBinaryFilePredicate = notBinaryFilePredicate;
    return this;
  }

  public Analyzer analyzeAllFilesMode(boolean analyzeAllFilesMode) {
    this.analyzeAllFilesMode = analyzeAllFilesMode;
    return this;
  }

  public void analyzeFiles(Collection<InputFile> inputFiles) {
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

        analyzeFile(inputFile);
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

  private void analyzeFile(InputFile inputFile) {
    try {
      var inputFileContext = new InputFileContext(sensorContext, inputFile);
      if (analyzeAllFilesMode) {
        analyzeWithNotBinaryFileCheck(inputFileContext);
      } else {
        analyzeAllChecks(inputFileContext);
      }
    } catch (IOException | RuntimeException e) {
      logAnalysisError(sensorContext, inputFile, e);
    }
  }

  private void analyzeWithNotBinaryFileCheck(InputFileContext inputFileContext) {
    if (notBinaryFilePredicate.apply(inputFileContext.getInputFile())) {
      if (inputFileContext.hasNonTextCharacters()) {
        excludeBinaryFileExtension(notBinaryFilePredicate, inputFileContext.getInputFile());
      } else {
        analyzeAllChecks(inputFileContext);
      }
    }
  }

  private void analyzeAllChecks(InputFileContext inputFileContext) {
    for (Check check : activeChecks) {
      check.analyze(inputFileContext);
    }
  }

  private void excludeBinaryFileExtension(NotBinaryFilePredicate notBinaryFilePredicate, InputFile inputFile) {
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
