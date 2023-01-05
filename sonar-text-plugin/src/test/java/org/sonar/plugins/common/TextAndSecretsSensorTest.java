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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.error.AnalysisError;
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.utils.log.LogTesterJUnit5;
import org.sonar.check.Rule;
import org.sonar.plugins.text.api.TextCheck;
import org.sonar.plugins.text.checks.BIDICharacterCheck;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.sonar.plugins.common.TestUtils.activeRules;
import static org.sonar.plugins.common.TestUtils.asString;
import static org.sonar.plugins.common.TestUtils.defaultSensorContext;
import static org.sonar.plugins.common.TestUtils.inputFile;
import static org.sonar.plugins.common.TestUtils.sensorContext;

class TextAndSecretsSensorTest {

  @RegisterExtension
  LogTesterJUnit5 logTester = new LogTesterJUnit5();

  @Test
  void describe() {
    DefaultSensorDescriptor descriptor = new DefaultSensorDescriptor();
    sensor(defaultSensorContext()).describe(descriptor);

    assertThat(descriptor.name()).isEqualTo("TextAndSecretsSensor");
    assertThat(descriptor.languages()).isEmpty();
    assertThat(descriptor.isProcessesFilesIndependently()).isTrue();
    assertThat(descriptor.ruleRepositories()).containsExactlyInAnyOrder("text", "secrets");
    assertThat(logTester.logs()).isEmpty();
  }

  @Test
  void should_raise_an_issue_when_a_secret_is_detected() throws IOException {
    SensorContextTester context = defaultSensorContext();
    context.fileSystem().add(inputFile(Path.of("src", "test", "resources", "checks", "GoogleCloudAccountKeyCheck", "GoogleCloudAccountPositive.json")));
    sensor(context).execute(context);

    assertThat(asString(context.allIssues())).containsExactly(
      "secrets:S6335 [5:18-5:1750] Make sure this Google Cloud service account key is not disclosed.");
    assertThat(logTester.logs()).containsExactly(
      "1 source file to be analyzed",
      "1/1 source file has been analyzed");
  }

  @Test
  void should_not_start_analysis_when_no_rule_is_active() throws IOException {
    String[] emptyActiveRuleList = {};
    SensorContextTester context = sensorContext(emptyActiveRuleList);
    context.fileSystem().add(inputFile(Path.of("src", "test", "resources", "checks", "GoogleCloudAccountKeyCheck", "GoogleCloudAccountPositive.json")));
    sensor(context).execute(context);

    assertThat(context.allIssues()).isEmpty();
    assertThat(logTester.logs()).isEmpty();
  }

  @Test
  void should_not_start_analysis_when_no_file_to_analyze() throws IOException {
    SensorContextTester context = defaultSensorContext();
    sensor(context).execute(context);

    assertThat(context.allIssues()).isEmpty();
    assertThat(logTester.logs()).isEmpty();
  }

  @Test
  void should_not_raise_an_issue_or_error_when_the_input_file_does_not_exist() {
    SensorContextTester context = defaultSensorContext();
    context.fileSystem().add(inputFile(Path.of("invalid-path.txt")));

    sensor(context).execute(context);

    assertThat(context.allIssues()).isEmpty();
    assertThat(logTester.logs()).anyMatch(log -> log.startsWith("Unable to analyze file") && log.contains("invalid-path.txt"));
  }

  @Test
  void empty_file_should_raise_no_issue() {
    SensorContextTester context = defaultSensorContext();
    analyse(sensor(context), context, inputFile(""));
    assertThat(context.allIssues()).isEmpty();
    assertThat(logTester.logs()).containsExactly(
      "1 source file to be analyzed",
      "1/1 source file has been analyzed");
  }

  @Rule(key = "IssueAtLineOne")
  class ReportIssueAtLineOneCheck extends TextCheck {
    public void analyze(InputFileContext ctx) {
      ctx.reportIssue(ruleKey, 1, "testIssue");
    }
  }

  @Test
  void valid_check_on_valid_file_should_raise_issue() {
    Check check = new ReportIssueAtLineOneCheck();
    InputFile inputFile = inputFile("foo");
    SensorContextTester context = defaultSensorContext();
    analyse(sensor(check), context, inputFile);

    assertThat(asString(context.allIssues())).containsExactly(
      "text:IssueAtLineOne [1:0-1:3] testIssue");
    assertThat(logTester.logs()).containsExactly(
      "1 source file to be analyzed",
      "1/1 source file has been analyzed");
  }

  @Test
  void stop_on_cancellation() {
    Check check = new ReportIssueAtLineOneCheck();
    SensorContextTester context = sensorContext(check);
    context.setCancelled(true);
    analyse(sensor(check), context, inputFile("{}"));
    assertThat(context.allIssues()).isEmpty();
    assertThat(logTester.logs()).containsExactly(
      "1 source file to be analyzed");
  }

  @Test
  void issue_should_not_be_raised_twice_on_same_line() {
    @Rule(key = "IssueAtLineOne")
    class ReportDuplicatedIssuesCheck extends TextCheck {
      public void analyze(InputFileContext ctx) {
        ctx.reportIssue(ruleKey, 1, "testIssue");
        ctx.reportIssue(ruleKey, 1, "testIssue");
      }
    }
    Check check = new ReportDuplicatedIssuesCheck();
    SensorContextTester context = sensorContext(check);
    InputFile inputFile = inputFile("foo");
    analyse(sensor(check), context, inputFile);

    assertThat(asString(context.allIssues())).containsExactly(
      "text:IssueAtLineOne [1:0-1:3] testIssue");
    assertThat(logTester.logs()).containsExactly(
      "1 source file to be analyzed",
      "1/1 source file has been analyzed");
  }

  @Test
  void file_should_not_be_handled_when_not_assigned_to_any_language() throws IOException {
    SensorContextTester context = defaultSensorContext();
    Path file = Path.of("src", "test", "resources", "checks", "BIDICharacterCheck", "test.php");
    InputFile inputFile = inputFile(file, Files.readString(file, UTF_8), null);
    analyse(sensor(context), context, inputFile);

    assertThat(context.allIssues()).isEmpty();
    assertThat(logTester.logs()).containsExactly(
      "1 source file to be analyzed",
      "1/1 source file has been analyzed");
  }

  @Test
  void analysis_error_should_be_raised_on_failure_in_check() {
    @Rule(key = "Crash")
    class CrashCheck extends TextCheck {
      public void analyze(InputFileContext ctx) {
        throw new IllegalStateException("Crash");
      }
    }
    TextCheck check = new CrashCheck();
    SensorContextTester context = sensorContext(check);
    InputFile inputFile = inputFile("foo");
    analyse(sensor(check), context, inputFile);

    Collection<AnalysisError> analysisErrors = context.allAnalysisErrors();
    assertThat(analysisErrors).hasSize(1);
    AnalysisError analysisError = analysisErrors.iterator().next();
    assertThat(analysisError.inputFile()).isEqualTo(inputFile);
    assertThat(analysisError.message()).startsWith("Unable to analyze");
    assertThat(logTester.logs()).anyMatch(log -> log.startsWith("Unable to analyze"));
  }

  @Test
  void should_not_execute_checks_on_binary_files() {
    @Rule(key = "Boom")
    class BoomCheck extends TextCheck {
      public void analyze(InputFileContext ctx) {
        throw new IllegalStateException("Should not occurs because there are only binary files");
      }
    }
    Check check = new BoomCheck();
    SensorContextTester context = sensorContext(check);
    Path binaryFile = Path.of("target", "test-classes", "org", "sonar", "plugins", "common", "InputFileContextTest.class");
    analyse(sensor(check), context, inputFile(binaryFile));

    assertThat(logTester.logs()).containsExactly(
      "1 source file to be analyzed",
      "1/1 source file has been analyzed");
  }

  @Test
  void analysis_error_should_be_raised_on_corrupted_file() throws IOException {
    Check check = new BIDICharacterCheck();
    SensorContextTester context = sensorContext(check);
    InputFile inputFile = spy(inputFile("{}"));
    when(inputFile.inputStream()).thenThrow(new IOException("Fail to read file input stream"));
    analyse(sensor(check), context, inputFile);

    Collection<AnalysisError> analysisErrors = context.allAnalysisErrors();
    assertThat(analysisErrors).hasSize(1);
    AnalysisError analysisError = analysisErrors.iterator().next();
    assertThat(analysisError.inputFile()).isEqualTo(inputFile);
    assertThat(analysisError.message()).startsWith("Unable to analyze").endsWith("Fail to read file input stream");
    assertThat(analysisError.location()).isNull();

    assertThat(logTester.logs()).anyMatch(log -> log.startsWith("Unable to analyze"));
  }

  private void analyse(Sensor sensor, SensorContextTester context, InputFile... inputFiles) {
    for (InputFile inputFile : inputFiles) {
      context.fileSystem().add(inputFile);
    }
    sensor.execute(context);
  }

  private TextAndSecretsSensor sensor(Check check) {
    CheckFactory checkFactory = new CheckFactory(activeRules(check.ruleKey.toString()));
    return new TextAndSecretsSensor(checkFactory) {
      @Override
      protected List<Check> getActiveChecks() {
        return Collections.singletonList(check);
      }
    };
  }

  private static TextAndSecretsSensor sensor(SensorContext sensorContext) {
    return new TextAndSecretsSensor(new CheckFactory(sensorContext.activeRules()));
  }

}
