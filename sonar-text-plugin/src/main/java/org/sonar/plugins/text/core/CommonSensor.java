/*
 * SonarQube Text Plugin
 * Copyright (C) 2021-2021 SonarSource SA
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
import org.sonar.plugins.text.CommonPlugin;
import org.sonar.plugins.text.checks.CheckList;
import org.sonar.plugins.text.visitor.ChecksVisitor;
import org.sonar.plugins.text.api.CommonCheck;
import org.sonarsource.analyzer.commons.ProgressReport;

public class CommonSensor implements Sensor {

  private final Checks<CommonCheck> checks;

  public CommonSensor(CheckFactory checkFactory) {
    this.checks = checkFactory.create(CommonPlugin.REPOSITORY_KEY);
    this.checks.addAnnotatedChecks((Iterable<?>) CheckList.checks());
  }

  @Override
  public void describe(SensorDescriptor sensorDescriptor) {
    sensorDescriptor.name("Common Sensor");
  }

  @Override
  public void execute(SensorContext sensorContext) {
    FileSystem fileSystem = sensorContext.fileSystem();
    Iterable<InputFile> inputFiles = fileSystem.inputFiles(fileSystem.predicates().all());
    List<String> filenames = StreamSupport.stream(inputFiles.spliterator(), false).map(InputFile::toString).collect(Collectors.toList());
    ProgressReport progressReport = new ProgressReport("Progress of the common analysis", TimeUnit.SECONDS.toMillis(10));
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
        checksVisitor.scan(inputFileContext);
        progressReport.nextFile();
      }
      return true;
    }
  }
}
