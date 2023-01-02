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
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.rule.Checks;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.plugins.secrets.NormalizedInputFile;
import org.sonar.plugins.secrets.rules.SecretCheckList;
import org.sonar.plugins.secrets.rules.SecretRule;
import org.sonar.plugins.secrets.rules.SecretsRulesDefinition;
import org.sonar.plugins.text.api.TextCheck;
import org.sonar.plugins.text.checks.TextCheckList;
import org.sonar.plugins.text.core.InputFileContext;
import org.sonar.plugins.text.rules.TextRuleDefinition;
import org.sonar.plugins.text.visitor.ChecksVisitor;
import org.sonarsource.analyzer.commons.ProgressReport;

public class TextAndSecretsSensor implements Sensor {

  private static final Logger LOG = Loggers.get(TextAndSecretsSensor.class);

  private final CheckFactory checkFactory;

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
    FileSystem fileSystem = sensorContext.fileSystem();
    List<InputFile> allInputFiles = new ArrayList<>();
    fileSystem.inputFiles(fileSystem.predicates().all()).forEach(allInputFiles::add);
    if (allInputFiles.isEmpty()) {
      return;
    }

    Checks<TextCheck> textChecks = checkFactory.create(TextRuleDefinition.REPOSITORY_KEY);
    textChecks.addAnnotatedChecks(TextCheckList.checks());
    ChecksVisitor textChecksVisitor = new ChecksVisitor(textChecks);

    List<SecretRule> secretsActiveRules = getSecretsActiveRules(sensorContext);

    List<String> filenames = allInputFiles.stream().map(InputFile::toString).collect(Collectors.toList());
    ProgressReport progressReport = new ProgressReport("Progress of the text and secrets analysis", TimeUnit.SECONDS.toMillis(10));
    progressReport.start(filenames);
    boolean success = true;
    try {
      for (InputFile inputFile : allInputFiles) {
        if (sensorContext.isCancelled()) {
          success = false;
          break;
        }
        InputFileContext inputFileContext = new InputFileContext(sensorContext, inputFile);
        try {
          inputFileContext.loadContent();
        } catch (IOException e) {
          logAnalysisError(inputFileContext, e);
          continue;
        }
        if (inputFileContext.isBinaryFile()) {
          continue;
        }
        if (inputFile.language() != null) {
          try {
            textChecksVisitor.scan(inputFileContext);
          } catch (RuntimeException e) {
            logAnalysisError(inputFileContext, e);
          }
        }
        try {
          NormalizedInputFile normalizedInputFile = new NormalizedInputFile(inputFile, inputFileContext.content());
          secretsActiveRules.forEach(rule ->
                  rule.findSecretsIn(normalizedInputFile)
                          .forEach(secret -> createSecretsIssue(sensorContext, rule.getRuleKey(), inputFile, secret.getTextRange(), rule.getMessage())));
        } catch (Exception e) {
          LOG.error("Can't analyze file", e);
        }
        progressReport.nextFile();
      }
    } finally {
      if (success) {
        progressReport.stop();
      } else {
        progressReport.cancel();
      }
    }

  }

  private static List<SecretRule> getSecretsActiveRules(SensorContext context) {
    return SecretCheckList.createInstances().stream()
            .filter(rule -> context.activeRules().find(RuleKey.of(SecretsRulesDefinition.REPOSITORY_KEY, rule.getRuleKey())) != null)
            .collect(Collectors.toList());
  }

  private static void createSecretsIssue(SensorContext context, String ruleKey, InputFile file, TextRange textRange, String message) {
    NewIssue newIssue = context.newIssue()
            .forRule(RuleKey.of(SecretsRulesDefinition.REPOSITORY_KEY, ruleKey));

    NewIssueLocation primaryLocation = newIssue.newLocation()
            .on(file)
            .at(textRange)
            .message(message);
    newIssue.at(primaryLocation);
    newIssue.save();
  }

  private static void logAnalysisError(InputFileContext inputFileContext, Exception e) {
    URI inputFileUri = inputFileContext.uri();
    String message = String.format("Unable to analyze file %s: %s", inputFileUri, e.getMessage());
    inputFileContext.reportAnalysisError(message);
    LOG.warn(message);
    LOG.debug(e.toString());
  }

}
