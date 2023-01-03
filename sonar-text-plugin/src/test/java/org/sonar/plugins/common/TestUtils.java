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
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.rule.internal.ActiveRulesBuilder;
import org.sonar.api.batch.rule.internal.NewActiveRule;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.batch.sensor.issue.Issue;
import org.sonar.api.batch.sensor.issue.IssueLocation;

import static java.nio.charset.StandardCharsets.UTF_8;

public class TestUtils {

  public static Collection<Issue> analyze(Check check, InputFile inputFile) throws IOException {
    SensorContextTester context = SensorContextTester.create(Paths.get("."));
    ActiveRulesBuilder activeRulesBuilder = new ActiveRulesBuilder();
    activeRulesBuilder.addRule(new NewActiveRule.Builder().setRuleKey(check.ruleKey()).build());
    context.setActiveRules(activeRulesBuilder.build());
    InputFileContext inputFileContext = new InputFileContext(context, inputFile);
    inputFileContext.loadContent();
    check.analyze(inputFileContext);
    return context.allIssues();
  }

  public static List<String> asString(Collection<Issue> issue) {
    return issue.stream().map(TestUtils::asString).collect(Collectors.toList());
  }

  public static String asString(Issue issue) {
    IssueLocation location = issue.primaryLocation();
    TextRange range = location.textRange();
    return issue.ruleKey() + " [" +
      range.start().line() + ":" + range.start().lineOffset() + "-" +
      range.end().line() + ":" + range.end().lineOffset() + "] " +
      location.message();
  }

  public static InputFile inputFile(String fileContent) {
    return inputFile(fileContent, "FileName.java");
  }

  public static InputFile inputFile(String fileContent, String fileName) {
    return new TestInputFileBuilder("", fileName)
      .setContents(fileContent)
      .setCharset(UTF_8)
      .setLanguage("java")
      .build();
  }

  public static InputFile inputFile(Path path, Charset encoding) throws IOException {
    return new TestInputFileBuilder("", path.toString())
      .setContents(Files.readString(path, encoding))
      .setCharset(encoding)
      .setLanguage("java")
      .build();
  }

}
