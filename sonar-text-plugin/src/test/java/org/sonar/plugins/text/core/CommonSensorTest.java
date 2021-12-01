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

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import javax.annotation.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.rule.Checks;
import org.sonar.api.batch.rule.internal.ActiveRulesBuilder;
import org.sonar.api.batch.rule.internal.NewActiveRule;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.error.AnalysisError;
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.batch.sensor.issue.Issue;
import org.sonar.api.batch.sensor.issue.IssueLocation;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.log.LogTesterJUnit5;
import org.sonar.plugins.text.CommonPlugin;
import org.sonar.plugins.text.api.CommonCheck;
import org.sonar.plugins.text.checks.AbstractCheck;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CommonSensorTest {

  @RegisterExtension
  public LogTesterJUnit5 logTester = new LogTesterJUnit5();

  private static final String REPOSITORY_KEY = CommonPlugin.REPOSITORY_KEY;

  @TempDir
  protected File baseDir;
  protected SensorContextTester context;

  @BeforeEach
  void setup() {
    context = SensorContextTester.create(baseDir);
    MapSettings settings = new MapSettings();
    context.setSettings(settings);
  }

  @Test
  void should_return_sensor_descriptor() {
    DefaultSensorDescriptor descriptor = new DefaultSensorDescriptor();
    sensor().describe(descriptor);
    assertThat(descriptor.name()).isEqualTo("Common Sensor");
    assertThat(descriptor.languages()).isEmpty();
  }

  @Test
  void empty_file_should_raise_no_issue() {
    // TODO replace by rules supported by this plugin
    analyse(sensor("S2260"), inputFile("empty.ts", ""));
    assertThat(context.allIssues()).as("No issue must be raised").isEmpty();
  }

  @Test
  void valid_check_on_valid_file_should_raise_issue() {
    CommonCheck validCheck = new AbstractCheck() {
      @Override
      public void analyze(InputFile inputFile) {
        ctx.reportLineIssue(1, "testIssue");
      }
    };
    CheckFactory checkFactory = mockCheckFactory(validCheck, "valid");

    InputFile inputFile = inputFile("file1.ts", "foo");
    analyse(sensor(checkFactory), inputFile);

    Collection<Issue> issues = context.allIssues();
    assertThat(issues).hasSize(1);
    Issue issue = issues.iterator().next();
    assertThat(issue.ruleKey().rule()).isEqualTo("valid");
    IssueLocation location = issue.primaryLocation();
    assertThat(location.inputComponent()).isEqualTo(inputFile);
    assertThat(location.message()).isEqualTo("testIssue");
    assertTextRange(location.textRange(), 1, 0, 1, 3);
  }

  @Test
  void stop_on_cancellation() {
    CommonCheck validCheck = new AbstractCheck() {
      @Override
      public void analyze(InputFile inputFile) {
        ctx.reportLineIssue(1, "testIssue");
      }
    };
    CheckFactory checkFactory = mockCheckFactory(validCheck, "valid");

    context.setCancelled(true);
    analyse(sensor(checkFactory), inputFile("file1.ts", "{}"));
    Collection<Issue> issues = context.allIssues();
    assertThat(issues).isEmpty();
  }

  @Test
  void issue_should_not_be_raised_twice_on_same_line() {
    CommonCheck validCheck = new AbstractCheck() {
      @Override
      public void analyze(InputFile inputFile) {
        ctx.reportLineIssue(1, "testIssue");
        ctx.reportLineIssue(1, "testIssue");
      }
    };
    CheckFactory checkFactory = mockCheckFactory(validCheck, "valid");

    InputFile inputFile = inputFile("file1.ts", "foo");
    analyse(sensor(checkFactory), inputFile);

    Collection<Issue> issues = context.allIssues();
    assertThat(issues).hasSize(1);
  }

  @Test
  void analysis_error_should_be_raised_on_failure_in_check() {
    CommonCheck failingCheck = new AbstractCheck() {
      @Override
      public void analyze(InputFile inputFile) {
        throw new IllegalStateException("Crash");
      }
    };
    CheckFactory checkFactory = mockCheckFactory(failingCheck, "failing");

    InputFile inputFile = inputFile("file1.iac", "foo");
    analyse(sensor(checkFactory), inputFile);

    Collection<AnalysisError> analysisErrors = context.allAnalysisErrors();
    assertThat(analysisErrors).hasSize(1);
    AnalysisError analysisError = analysisErrors.iterator().next();
    assertThat(analysisError.inputFile()).isEqualTo(inputFile);
    assertThat(analysisError.message()).startsWith("Unable to analyze");
    assertThat(logTester.logs()).anyMatch(log -> log.startsWith("Unable to analyze"));
  }

  private void assertTextRange(@Nullable TextRange actual, int startLine, int startLineOffset, int endLine, int endLineOffset) {
    assertThat(actual).isNotNull();
    assertThat(actual.start().line()).as("startLine mismatch").isEqualTo(startLine);
    assertThat(actual.start().lineOffset()).as("startLineOffset mismatch").isEqualTo(startLineOffset);
    assertThat(actual.end().line()).as("endLine mismatch").isEqualTo(endLine);
    assertThat(actual.end().lineOffset()).as("endLineOffset mismatch").isEqualTo(endLineOffset);
  }

  private void analyse(Sensor sensor, InputFile... inputFiles) {
    for (InputFile inputFile : inputFiles) {
      context.fileSystem().add(inputFile);
    }
    sensor.execute(context);
  }

  private CommonSensor sensor(String... rules) {
    return sensor(checkFactory(rules));
  }

  private CommonSensor sensor(CheckFactory checkFactory) {
    return new CommonSensor(checkFactory);
  }

  private CheckFactory mockCheckFactory(CommonCheck check, String ruleName) {
    CheckFactory checkFactory = mock(CheckFactory.class);
    Checks checks = mock(Checks.class);
    when(checks.ruleKey(check)).thenReturn(RuleKey.of(REPOSITORY_KEY, ruleName));
    when(checkFactory.create(REPOSITORY_KEY)).thenReturn(checks);
    when(checks.all()).thenReturn(Collections.singletonList(check));
    return checkFactory;
  }

  private CheckFactory checkFactory(String... ruleKeys) {
    ActiveRulesBuilder builder = new ActiveRulesBuilder();
    for (String ruleKey : ruleKeys) {
      NewActiveRule newRule = new NewActiveRule.Builder()
        .setRuleKey(RuleKey.of(REPOSITORY_KEY, ruleKey))
        .setName(ruleKey)
        .build();
      builder.addRule(newRule);
    }
    context.setActiveRules(builder.build());
    return new CheckFactory(context.activeRules());
  }

  private InputFile inputFile(String relativePath, String content) {
    return inputFile(relativePath, content, "");
  }

  private InputFile inputFile(String relativePath, String content, String language) {
    return new TestInputFileBuilder("moduleKey", relativePath)
      .setModuleBaseDir(baseDir.toPath())
      .setType(InputFile.Type.MAIN)
      .setLanguage(language)
      .setCharset(StandardCharsets.UTF_8)
      .setContents(content)
      .build();
  }
}
