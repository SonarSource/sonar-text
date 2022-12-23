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
package org.sonar.plugins.secrets;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.stream.Collectors;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.plugins.secrets.rules.SecretCheckList;
import org.sonar.plugins.secrets.rules.SecretRule;
import org.sonar.plugins.secrets.rules.SecretsRulesDefinition;

public class SecretsSensor implements Sensor {

  private static final Logger LOG = Loggers.get(SecretsSensor.class);

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor.name("Sonar Secrets Detection Sensor");
    descriptor.createIssuesForRuleRepositories(SecretsRulesDefinition.REPOSITORY_KEY);
  }

  @Override
  public void execute(SensorContext context) {
    List<SecretRule> activeRules = getActiveRules(context);
    for (InputFile file : getAllInputFiles(context)) {
      try {
        NormalizedInputFile normalizedInputFile = read(file);
        activeRules.forEach(rule ->
          rule.findSecretsIn(normalizedInputFile)
            .forEach(secret -> createIssue(context, rule.getRuleKey(), file, secret.getTextRange(), rule.getMessage())));
      } catch (Exception e) {
        LOG.error("Can't analyze file", e);
      }
    }
  }

  private static List<SecretRule> getActiveRules(SensorContext context) {
    return SecretCheckList.createInstances().stream()
      .filter(rule -> context.activeRules().find(RuleKey.of(SecretsRulesDefinition.REPOSITORY_KEY, rule.getRuleKey())) != null)
      .collect(Collectors.toList());
  }

  private static Iterable<InputFile> getAllInputFiles(SensorContext context) {
    FileSystem fs = context.fileSystem();
    return fs.inputFiles(fs.predicates().all());
  }

  private static NormalizedInputFile read(InputFile file) throws IOException {
    BufferedReader reader = new BufferedReader(new InputStreamReader(file.inputStream(), file.charset()));
    return new NormalizedInputFile(file, reader.lines().collect(Collectors.joining("\n")));
  }

  private static void createIssue(SensorContext context, String ruleKey, InputFile file, TextRange textRange, String message) {
    NewIssue newIssue = context.newIssue()
      .forRule(RuleKey.of(SecretsRulesDefinition.REPOSITORY_KEY, ruleKey));

    NewIssueLocation primaryLocation = newIssue.newLocation()
      .on(file)
      .at(textRange)
      .message(message);
    newIssue.at(primaryLocation);
    newIssue.save();
  }
}
