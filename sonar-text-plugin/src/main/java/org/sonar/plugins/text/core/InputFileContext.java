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

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.error.NewAnalysisError;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.rule.RuleKey;

public class InputFileContext {

  public final SensorContext sensorContext;
  public final InputFile inputFile;
  private final Set<Integer> raisedIssues = new HashSet<>();

  public InputFileContext(SensorContext sensorContext, InputFile inputFile) {
    this.sensorContext = sensorContext;
    this.inputFile = inputFile;
  }

  public void reportLineIssue(RuleKey ruleKey, int line, String message) {
    if (raisedIssues.add(issueHash(ruleKey, line))) {
      NewIssue issue = sensorContext.newIssue();
      NewIssueLocation issueLocation = issue.newLocation()
        .on(inputFile)
        .at(inputFile.selectLine(line))
        .message(message);
      issue.forRule(ruleKey).at(issueLocation);
      issue.save();
    }
  }

  public void reportAnalysisError(String message) {
    NewAnalysisError error = sensorContext.newAnalysisError();
    error
      .message(message)
      .onFile(inputFile);

    error.save();
  }

  private static int issueHash(RuleKey ruleKey, int line) {
    return Objects.hash(ruleKey, line);
  }
}
