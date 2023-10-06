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

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sonar.api.SonarProduct;
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.plugins.secrets.SecretsRulesDefinition;
import org.sonar.plugins.secrets.api.SpecificationBasedCheck;
import org.sonar.plugins.secrets.api.SpecificationLoader;
import org.sonar.plugins.text.TextRuleDefinition;

public class TextAndSecretsSensor implements Sensor {

  public static final String EXCLUDED_FILE_SUFFIXES_KEY = "sonar.text.excluded.file.suffixes";
  private static final String ANALYZE_ALL_FILES_KEY = "sonar.text.analyzeAllFiles";
  public static final String INCLUDED_FILE_SUFFIXES_KEY = "sonar.text.included.file.suffixes";
  public static final String INCLUDED_FILE_SUFFIXES_DEFAULT_VALUE = "sh,bash,zsh,ksh,ps1,yaml,yml,properties,conf,xml,pem,env,config";

  public static final String TEXT_CATEGORY = "Secrets";

  private static final FilePredicate LANGUAGE_FILE_PREDICATE = inputFile -> inputFile.language() != null;

  private final CheckFactory checkFactory;

  private DurationStatistics durationStatistics;

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
    // Retrieve list of checks
    List<Check> activeChecks = getActiveChecks();
    durationStatistics = new DurationStatistics(sensorContext.config());
    initializeSpecificationBasedChecks(activeChecks);
    if (activeChecks.isEmpty()) {
      return;
    }

    // Retrieve list of files to analyse using the right FilePredicate
    boolean analyzeAllFiles = isSonarLintContext(sensorContext) || analyzeAllFiles(sensorContext);
    var notBinaryFilePredicate = notBinaryFilePredicate(sensorContext);
    var textFilePredicate = plaintextFilePredicate(sensorContext);
    FilePredicate filePredicate = analyzeAllFiles ? notBinaryFilePredicate : sensorContext.fileSystem().predicates().or(LANGUAGE_FILE_PREDICATE, textFilePredicate);
    List<InputFile> inputFiles = getInputFiles(sensorContext, filePredicate);
    if (inputFiles.isEmpty()) {
      return;
    }

    Analyzer.analyzeFiles(sensorContext, activeChecks, notBinaryFilePredicate, analyzeAllFiles, inputFiles);
    durationStatistics.log();
  }

  /**
   * Blacklist approach: provide a predicate that exclude file that are considered as not-binary file.
   * Example: for 'exe', 'txt' and 'unknown', it will return true for 'txt' and 'unknown'
   * List of binary extension to exclude are provided by configuration key {@link TextAndSecretsSensor#EXCLUDED_FILE_SUFFIXES_KEY}
   */
  private static NotBinaryFilePredicate notBinaryFilePredicate(SensorContext sensorContext) {
    return new NotBinaryFilePredicate(sensorContext.config().getStringArray(TextAndSecretsSensor.EXCLUDED_FILE_SUFFIXES_KEY));
  }

  /**
   * Whitelist approach: provide a predicate that include file that are considered as text file.
   * Example: for 'exe', 'txt' and 'unknown', it will return true for 'txt'
   * List of text extension to include are provided by configuration key {@link TextAndSecretsSensor#INCLUDED_FILE_SUFFIXES_KEY}
   */
  private static FilePredicate plaintextFilePredicate(SensorContext sensorContext) {
    String[] plaintextFileExtensions = sensorContext.config().getStringArray(TextAndSecretsSensor.INCLUDED_FILE_SUFFIXES_KEY);
    if (plaintextFileExtensions.length == 0) {
      return sensorContext.fileSystem().predicates().none();
    }

    List<FilePredicate> extensionPredicates = new ArrayList<>();
    for (String extension : plaintextFileExtensions) {
      var filePredicate = sensorContext.fileSystem().predicates().hasExtension(extension);
      extensionPredicates.add(filePredicate);
    }
    return sensorContext.fileSystem().predicates().or(extensionPredicates);
  }

  private static boolean isSonarLintContext(SensorContext sensorContext) {
    return sensorContext.runtime().getProduct().equals(SonarProduct.SONARLINT);
  }

  private static boolean analyzeAllFiles(SensorContext sensorContext) {
    return "true".equals(sensorContext.config().get(ANALYZE_ALL_FILES_KEY).orElse("false"));
  }

  /**
   * In SonarLint context we want to analyze all non-binary input files, even when they are not analyzed or assigned to a language.
   * To avoid analyzing all non-binary files to reduce time and memory consumption in a non SonarLint context only files assigned to a
   * language OR file with a text extension are analyzed.
   */
  private static List<InputFile> getInputFiles(SensorContext sensorContext, FilePredicate filePredicate) {
    List<InputFile> inputFiles = new ArrayList<>();
    FileSystem fileSystem = sensorContext.fileSystem();
    for (InputFile inputFile : fileSystem.inputFiles(filePredicate)) {
      inputFiles.add(inputFile);
    }
    return inputFiles;
  }

  protected List<Check> getActiveChecks() {
    List<Check> checks = new ArrayList<>();
    checks.addAll(checkFactory.<Check>create(TextRuleDefinition.REPOSITORY_KEY)
      .addAnnotatedChecks(TextRuleDefinition.checks()).all());
    checks.addAll(checkFactory.<Check>create(SecretsRulesDefinition.REPOSITORY_KEY)
      .addAnnotatedChecks(SecretsRulesDefinition.checks()).all());
    return checks;
  }

  protected void initializeSpecificationBasedChecks(List<Check> checks) {
    SpecificationLoader specificationLoader = new SpecificationLoader();
    Map<URI, List<TextRange>> reportedIssuesForCtx = new HashMap<>();
    for (Check activeCheck : checks) {
      if (activeCheck instanceof SpecificationBasedCheck) {
        ((SpecificationBasedCheck) activeCheck).initialize(specificationLoader, reportedIssuesForCtx, durationStatistics);
      }
    }
  }
}
