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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.rule.internal.ActiveRulesBuilder;
import org.sonar.api.batch.rule.internal.DefaultActiveRules;
import org.sonar.api.batch.rule.internal.NewActiveRule;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.batch.sensor.issue.Issue;
import org.sonar.api.batch.sensor.issue.IssueLocation;
import org.sonar.api.rule.RuleKey;
import org.sonar.plugins.secrets.SecretsRulesDefinition;
import org.sonar.plugins.text.TextRuleDefinition;

import static java.nio.charset.StandardCharsets.UTF_8;

public class TestUtils {

  public static List<String> analyze(Check check, String fileContent) throws IOException {
    return analyze(check, inputFile(fileContent));
  }

  public static List<String> analyze(Check check, InputFile inputFile) throws IOException {
    SensorContextTester context = sensorContext(check);
    InputFileContext inputFileContext = new InputFileContext(context, inputFile);
    check.analyze(inputFileContext);
    return asString(context.allIssues());
  }

  public static List<String> asString(Collection<Issue> issues) {
    return issues.stream().map(TestUtils::asString).collect(Collectors.toList());
  }

  public static String asString(Issue issue) {
    IssueLocation location = issue.primaryLocation();
    TextRange range = location.textRange();
    return String.format("%s [%d:%d-%d:%d] %s", issue.ruleKey(), range.start().line(), range.start().lineOffset(),
      range.end().line(), range.end().lineOffset(), location.message());
  }

  public static InputFile inputFile(String fileContent) {
    return inputFile(Path.of("file.txt"), fileContent, "text");
  }

  public static InputFile inputFile(Path path) {
    String content = null;
    try {
      content = Files.readString(path, UTF_8);
    } catch (IOException e) {
      // ignored, so InputFile.inputStream() will fail
    }
    return inputFile(path, content, "text");
  }

  public static InputFile inputFile(Path path, @Nullable String content, @Nullable String language) {
    TestInputFileBuilder builder = new TestInputFileBuilder(".", path.toString())
      .setType(InputFile.Type.MAIN)
      .setLanguage(language)
      .setCharset(UTF_8);
    if (content != null) {
      builder.setContents(content);
    }
    return builder.build();
  }

  public static DefaultActiveRules activeRules(String... ruleKeys) {
    ActiveRulesBuilder builder = new ActiveRulesBuilder();
    for (String ruleId : ruleKeys) {
      RuleKey ruleKey = RuleKey.parse(ruleId);
      builder.addRule(new NewActiveRule.Builder()
        .setRuleKey(ruleKey)
        .setName(ruleKey.rule())
        .build());
    }
    return builder.build();
  }

  public static String[] allRuleKeys() {
    return Stream.of(SecretsRulesDefinition.checks(), TextRuleDefinition.checks())
      .flatMap(Collection::stream)
      .map(checkClass -> {
        try {
          return ((Check) checkClass.getDeclaredConstructor().newInstance()).ruleKey.toString();
        } catch (ReflectiveOperationException e) {
          throw new RuntimeException(e);
        }
      })
      .toArray(String[]::new);
  }

  public static SensorContextTester sensorContext(Check check) {
    return sensorContext(check.ruleKey.toString());
  }

  public static SensorContextTester defaultSensorContext() {
    return sensorContext(allRuleKeys());
  }

  public static SensorContextTester sensorContext(String... activeRules) {
    return sensorContext(new File(".").getAbsoluteFile(), activeRules);
  }

  public static SensorContextTester sensorContext(File baseDir, String... activeRules) {
    return SensorContextTester.create(baseDir)
      .setActiveRules(activeRules(activeRules));
  }

}
