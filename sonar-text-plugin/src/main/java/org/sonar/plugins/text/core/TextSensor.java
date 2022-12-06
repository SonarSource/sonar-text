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
package org.sonar.plugins.text.core;

import java.net.URI;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.rule.Checks;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.plugins.text.TextPlugin;
import org.sonar.plugins.text.api.TextCheck;
import org.sonar.plugins.text.checks.CheckList;
import org.sonar.plugins.text.visitor.ChecksVisitor;
import org.sonarsource.analyzer.commons.ProgressReport;

public class TextSensor implements Sensor {

  private static final Logger LOG = Loggers.get(TextSensor.class);

  private final Checks<TextCheck> checks;

  public TextSensor(CheckFactory checkFactory) {
    this.checks = checkFactory.create(TextPlugin.REPOSITORY_KEY);
    this.checks.addAnnotatedChecks(CheckList.checks());
  }

  @Override
  public void describe(SensorDescriptor sensorDescriptor) {
    sensorDescriptor
      .name("Text Sensor")
      .processesFilesIndependently();
  }

  @Override
  public void execute(SensorContext sensorContext) {
    FileSystem fileSystem = sensorContext.fileSystem();
    Iterable<InputFile> inputFiles = fileSystem.inputFiles(inputFile -> inputFile.language() != null);
    List<String> filenames = StreamSupport.stream(inputFiles.spliterator(), false).map(InputFile::toString).collect(Collectors.toList());
    ProgressReport progressReport = new ProgressReport("Progress of the text analysis", TimeUnit.SECONDS.toMillis(10));
    progressReport.start(filenames);
    boolean success = false;
    Analyzer analyzer = new Analyzer(new ChecksVisitor(checks));
    try {
      success = analyzer.analyseFiles(sensorContext, inputFiles, progressReport);
    } finally {
      if (success) {
        progressReport.stop();
      } else {
        progressReport.cancel();
      }
    }
  }

  private static class Analyzer {

    private final ChecksVisitor checksVisitor;

    public Analyzer(ChecksVisitor checksVisitor) {
      this.checksVisitor = checksVisitor;
    }

    boolean analyseFiles(SensorContext sensorContext, Iterable<InputFile> inputFiles, ProgressReport progressReport) {
      for (InputFile inputFile : inputFiles) {
        if (sensorContext.isCancelled()) {
          return false;
        }
        InputFileContext inputFileContext = new InputFileContext(sensorContext, inputFile);
        try {
          checksVisitor.scan(inputFileContext);
        } catch (RuntimeException e) {
          logAnalysisError(inputFileContext, e);
        }
        progressReport.nextFile();
      }
      return true;
    }

    private static void logAnalysisError(InputFileContext inputFileContext, Exception e) {
      URI inputFileUri = inputFileContext.inputFile.uri();
      String message = String.format("Unable to analyze file %s: %s", inputFileUri, e.getMessage());
      inputFileContext.reportAnalysisError(message);
      LOG.warn(message);
      LOG.debug(e.toString());
    }
  }
}
