/*
 * SonarQube Text Plugin
 * Copyright (C) 2021-2022 SonarSource SA
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.plugins.secrets.SecretsRulesDefinition;
import org.sonar.plugins.text.TextRuleDefinition;
import org.sonarsource.analyzer.commons.ProgressReport;

public class TextAndSecretsSensor implements Sensor {

  private static final Logger LOG = Loggers.get(TextAndSecretsSensor.class);

  public static final String EXCLUDED_FILE_SUFFIXES_KEY = "sonar.text.excluded.file.suffixes";

  public static final String TEXT_CATEGORY = "text";

  private final CheckFactory checkFactory;

  private boolean displayHelpAboutExcludingBinaryFile = true;

  public TextAndSecretsSensor(CheckFactory checkFactory) {
    this.checkFactory = checkFactory;
  }

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor
      .name("TextAndSecretsSensor")
      .createIssuesForRuleRepositories(TextRuleDefinition.REPOSITORY_KEY, SecretsRulesDefinition.REPOSITORY_KEY)
      .processesFilesIndependently();
  }

  @Override
  public void execute(SensorContext sensorContext) {
    List<Check> activeChecks = getActiveChecks();
    if (activeChecks.isEmpty()) {
      return;
    }

    BinaryFilePredicate binaryFilePredicate = binaryFilePredicate(sensorContext);
    List<InputFile> allInputFiles = getAllInputFiles(sensorContext, binaryFilePredicate);
    if (allInputFiles.isEmpty()) {
      return;
    }

    List<String> filenames = allInputFiles.stream().map(InputFile::toString).collect(Collectors.toList());
    ProgressReport progressReport = new ProgressReport("Progress of the text and secrets analysis", TimeUnit.SECONDS.toMillis(10));
    progressReport.start(filenames);
    boolean cancelled = false;
    try {
      for (InputFile inputFile : allInputFiles) {
        if (sensorContext.isCancelled()) {
          cancelled = true;
          break;
        }
        analyze(sensorContext, activeChecks, inputFile, binaryFilePredicate);
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

  private static BinaryFilePredicate binaryFilePredicate(SensorContext sensorContext) {
    return new BinaryFilePredicate(sensorContext.config().getStringArray(TextAndSecretsSensor.EXCLUDED_FILE_SUFFIXES_KEY));
  }

  private static List<InputFile> getAllInputFiles(SensorContext sensorContext, BinaryFilePredicate binaryFilePredicate) {
    List<InputFile> allInputFiles = new ArrayList<>();
    FileSystem fileSystem = sensorContext.fileSystem();
    for (InputFile inputFile : fileSystem.inputFiles(fileSystem.predicates().all())) {
      if (!binaryFilePredicate.hasBinaryFileName(inputFile.filename())) {
        allInputFiles.add(inputFile);
      }
    }
    return allInputFiles;
  }

  private void analyze(SensorContext sensorContext, List<Check> activeChecks, InputFile inputFile, BinaryFilePredicate binaryFilePredicate) {
    if (!binaryFilePredicate.hasBinaryFileName(inputFile.filename())) {
      try {
        InputFileContext inputFileContext = new InputFileContext(sensorContext, inputFile);
        if (inputFileContext.hasNonTextCharacters()) {
          excludeBinaryFileExtension(binaryFilePredicate, inputFile);
        } else {
          for (Check check : activeChecks) {
            check.analyze(inputFileContext);
          }
        }
      } catch (IOException | RuntimeException e) {
        logAnalysisError(sensorContext, inputFile, e);
      }
    }
  }

  private void excludeBinaryFileExtension(BinaryFilePredicate binaryFilePredicate, InputFile inputFile) {
    String extension = BinaryFilePredicate.extension(inputFile.filename());
    if (extension != null) {
      binaryFilePredicate.addBinaryFileExtension(extension);
      LOG.warn("'{}' was added to the binary file filter because the file '{}' is a binary file.", extension, inputFile);
      if (displayHelpAboutExcludingBinaryFile) {
        displayHelpAboutExcludingBinaryFile = false;
        LOG.info("To remove the previous warning you can add the '.{}' extension to the '{}' property.", extension, TextAndSecretsSensor.EXCLUDED_FILE_SUFFIXES_KEY);
      }
    }
  }

  protected List<Check> getActiveChecks() {
    List<Check> checks = new ArrayList<>();
    checks.addAll(checkFactory.<Check>create(TextRuleDefinition.REPOSITORY_KEY)
      .addAnnotatedChecks(TextRuleDefinition.checks()).all());
    checks.addAll(checkFactory.<Check>create(SecretsRulesDefinition.REPOSITORY_KEY)
      .addAnnotatedChecks(SecretsRulesDefinition.checks()).all());
    return checks;
  }

  private static void logAnalysisError(SensorContext sensorContext, InputFile inputFile, Exception e) {
    String message = String.format("Unable to analyze file %s: %s", inputFile, e.getMessage());
    sensorContext.newAnalysisError()
      .message(message)
      .onFile(inputFile)
      .save();
    LOG.warn(message);
    LOG.debug(e.toString());
  }

}