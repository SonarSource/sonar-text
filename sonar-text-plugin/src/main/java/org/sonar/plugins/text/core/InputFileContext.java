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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.error.NewAnalysisError;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.rule.RuleKey;
import org.sonar.plugins.common.BinaryFileUtils;

public class InputFileContext {

  private final SensorContext sensorContext;
  private final InputFile inputFile;

  private boolean isBinaryFile;

  public List<String> lines;
  public String normalizedContent;

  private final Set<String> raisedIssues = new HashSet<>();

  public InputFileContext(SensorContext sensorContext, InputFile inputFile) {
    this.sensorContext = sensorContext;
    this.inputFile = inputFile;
    isBinaryFile = false;
    lines = Collections.emptyList();
    normalizedContent = "";
  }

  public void loadContent() throws IOException {
    List<String> contentLines = new ArrayList<>();
    BufferedReader reader = new BufferedReader(new InputStreamReader(inputFile.inputStream(), inputFile.charset()));
    String line = reader.readLine();
    while (line != null) {
      if (BinaryFileUtils.hasControlCharacters(line)) {
        isBinaryFile = true;
        return;
      }
      contentLines.add(line);
      line = reader.readLine();
    }
    isBinaryFile = false;
    lines = contentLines;
    normalizedContent = String.join("\n", contentLines);
  }

  public boolean isBinaryFile() {
    return isBinaryFile;
  }

  public String content() {
    return normalizedContent;
  }

  public List<String> lines() {
    return lines;
  }

  public URI uri() {
    return inputFile.uri();
  }

  public void reportLineIssue(RuleKey ruleKey, int line, String message) {
    if (raisedIssues.add(ruleKey + ":" + line)) {
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

}
