/*
 * SonarQube Text Plugin
 * Copyright (C) 2021-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.plugins.common;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.parallel.Isolated;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.event.Level;
import org.sonar.api.SonarRuntime;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.error.AnalysisError;
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.batch.sensor.issue.Issue;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonar.check.Rule;
import org.sonar.plugins.common.analyzer.Analyzer;
import org.sonar.plugins.common.git.GitService;
import org.sonar.plugins.common.measures.TelemetryReporter;
import org.sonar.plugins.secrets.AbstractBinaryFileCheck;
import org.sonar.plugins.secrets.SecretsRulesDefinition;
import org.sonar.plugins.secrets.api.SecretsSpecificationLoader;
import org.sonar.plugins.secrets.api.SpecificationBasedCheck;
import org.sonar.plugins.secrets.api.task.RegexMatchingManager;
import org.sonar.plugins.text.api.TextCheck;
import org.sonar.plugins.text.checks.BIDICharacterCheck;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.plugins.common.TestUtils.SONARCLOUD_RUNTIME;
import static org.sonar.plugins.common.TestUtils.SONARLINT_RUNTIME;
import static org.sonar.plugins.common.TestUtils.SONARQUBE_RUNTIME_WITHOUT_HIDDEN_FILES_SUPPORT;
import static org.sonar.plugins.common.TestUtils.SONARQUBE_RUNTIME_WITHOUT_TELEMETRY_SUPPORT;
import static org.sonar.plugins.common.TestUtils.asString;
import static org.sonar.plugins.common.TestUtils.inputFile;
import static org.sonar.plugins.common.TestUtils.inputFileFromPath;
import static org.sonar.plugins.common.TestUtils.sensorContext;
import static org.sonar.plugins.common.TextAndSecretsSensor.SONAR_TESTS_KEY;
import static org.sonar.plugins.common.TextAndSecretsSensor.TEXT_INCLUSIONS_DEFAULT_VALUE;

// The class is executed in isolation from other test classes, because of shouldNotLeakThreads() method.
@Isolated
public abstract class AbstractTextAndSecretsSensorTest {

  private static final String SENSITIVE_BIDI_CHARS = "\u0002\u0004";
  private static final int AVAILABLE_PROCESSORS = Runtime.getRuntime().availableProcessors();
  private static final String EXPECTED_PROCESSOR_LOG_LINE = "Available processors: " + AVAILABLE_PROCESSORS;
  private static final String DEFAULT_THREAD_USAGE_LOG_LINE = "Using " + AVAILABLE_PROCESSORS + " threads for analysis.";

  private static final String EXPECTED_SONAR_TEST_NOT_SET_LOG_LINE = """
    The property "sonar.tests" is not set. To improve the analysis accuracy, we categorize a file as a test file if any of the following is true:
      * The filename starts with "test"
      * The filename contains "test." or "tests."
      * Any directory in the file path is named: "doc", "docs", "test" or "tests"
      * Any directory in the file path has a name ending in "test" or "tests"
    """;

  @RegisterExtension
  protected LogTesterJUnit5 logTester = new LogTesterJUnit5();

  @AfterEach
  public void cleanUp() {
    int defaultTimeout = 10_000;
    // due to running other tests, this property can be changed. That's why we need to set the default after each test.
    RegexMatchingManager.setTimeoutMs(defaultTimeout);
    RegexMatchingManager.setUninterruptibleTimeoutMs(defaultTimeout);
  }

  protected abstract TextAndSecretsSensor sensor(Check... check);

  protected abstract TextAndSecretsSensor sensor(SensorContext sensorContext);

  protected abstract TestUtils testUtils();

  protected abstract String sensorName();

  @Test
  public void shouldDescribeWithoutErrors() {
    DefaultSensorDescriptor descriptor = new DefaultSensorDescriptor();
    SensorContextTester sensorContext = testUtils().defaultSensorContext();
    sensorContext.setRuntime(SONARQUBE_RUNTIME_WITHOUT_HIDDEN_FILES_SUPPORT);
    sensor(sensorContext).describe(descriptor);

    assertThat(descriptor.name()).isEqualTo(sensorName());
    assertThat(descriptor.languages()).isEmpty();
    assertThat(descriptor.isProcessesFilesIndependently()).isTrue();
    assertThat(descriptor.ruleRepositories()).containsExactlyInAnyOrder("text", "secrets");
    assertThat(logTester.logs()).isEmpty();
  }

  @Test
  public void descriptorShouldNotSignalHiddenFileProcessingWhenAPIDoesNotSupportIt() {
    // After release of SQS 25.3, we can use new DefaultSensorDescriptor() for this test, as then the mocking of SensorDescriptor is not needed
    // anymore.
    SensorDescriptor descriptor = mock(SensorDescriptor.class);
    SensorContextTester sensorContext = testUtils().defaultSensorContext();
    sensorContext.setRuntime(SONARQUBE_RUNTIME_WITHOUT_HIDDEN_FILES_SUPPORT);

    when(descriptor.name(any())).thenReturn(descriptor);
    when(descriptor.createIssuesForRuleRepository(any())).thenReturn(descriptor);
    when(descriptor.createIssuesForRuleRepositories(any(), any())).thenReturn(descriptor);
    when(descriptor.processesFilesIndependently()).thenReturn(descriptor);
    when(descriptor.global()).thenReturn(descriptor);
    sensor(sensorContext).describe(descriptor);

    verify(descriptor, never()).processesHiddenFiles();
    assertThat(logTester.logs()).isEmpty();
  }

  @Test
  public void descriptorShouldSignalHiddenFileProcessingWhenAPIDoesSupportIt() {
    // After release of SQS 25.3, we can integrate this test into shouldDescribeWithoutErrors(), as then the mocking of SensorDescriptor is not
    // needed anymore.
    SensorDescriptor descriptor = mock(SensorDescriptor.class);
    SensorContextTester sensorContext = testUtils().defaultSensorContext();

    when(descriptor.name(any())).thenReturn(descriptor);
    when(descriptor.createIssuesForRuleRepository(any())).thenReturn(descriptor);
    when(descriptor.createIssuesForRuleRepositories(any(), any())).thenReturn(descriptor);
    when(descriptor.processesFilesIndependently()).thenReturn(descriptor);
    when(descriptor.global()).thenReturn(descriptor);
    sensor(sensorContext).describe(descriptor);

    verify(descriptor).processesHiddenFiles();
    assertThat(logTester.logs()).isEmpty();
  }

  @Test
  void shouldNotStartAnalysisWhenNoRuleIsActive() {
    String[] emptyActiveRuleList = {};
    SensorContextTester context = sensorContext(emptyActiveRuleList);
    context.fileSystem().add(inputFileFromPath(Path.of("src", "test", "resources", "checks", "GoogleCloudAccountKeyCheck",
      "GoogleCloudAccountPositive.json")));
    sensor(context).execute(context);

    assertThat(context.allIssues()).isEmpty();
    assertCorrectLogsForTextAndSecretsAnalysis(0, false);
  }

  @Test
  void shouldNotStartAnalysisWhenNoFileToAnalyze() {
    SensorContextTester context = testUtils().defaultSensorContext();
    sensor(context).execute(context);

    assertThat(context.allIssues()).isEmpty();
    assertCorrectLogsForTextAndSecretsAnalysis(0, false);
  }

  @Test
  void shouldNotRaiseAnIssueOrErrorWhenTheInputFileDoesNotExist() {
    SensorContextTester context = testUtils().defaultSensorContext();
    context.fileSystem().add(inputFileFromPath(Path.of("invalid-path.txt")));

    sensor(context).execute(context);

    assertThat(context.allIssues()).isEmpty();
    assertThat(logTester.logs()).anyMatch(log -> log.startsWith("Unable to analyze file") && log.contains("invalid-path.txt"));
  }

  @Test
  void emptyFileShouldRaiseNoIssue() {
    SensorContextTester context = testUtils().defaultSensorContext();
    analyse(sensor(context), context, inputFile(""));
    assertThat(context.allIssues()).isEmpty();
    assertCorrectLogsForTextAndSecretsAnalysis(1, false);
  }

  @Test
  void emptyFileShouldRaiseNoIssueWithAutoTestDetectionEnabledBecauseSonarqube() {
    SensorContextTester context = testUtils().sonarqubeSensorContext();
    analyse(sensor(context), context, inputFile(""));
    assertThat(context.allIssues()).isEmpty();
    assertCorrectLogsForTextAndSecretsAnalysis(1, true);
  }

  @Rule(key = "IssueAtLineOne")
  public static class ReportIssueAtLineOneCheck extends TextCheck {
    public void analyze(InputFileContext ctx) {
      ctx.reportTextIssue(getRuleKey(), 1, "testIssue");
    }
  }

  @Test
  void validCheckOnValidFileShouldRaiseIssue() {
    Check check = new ReportIssueAtLineOneCheck();
    InputFile inputFile = inputFile("foo");
    SensorContextTester context = testUtils().defaultSensorContext();
    analyse(sensor(check), context, inputFile);

    assertThat(asString(context.allIssues())).containsExactly(
      "text:IssueAtLineOne [1:0-1:3] testIssue");
    assertCorrectLogsForTextAndSecretsAnalysis(1, false);
  }

  @Test
  void shouldStopOnCancellation() {
    Check check = new ReportIssueAtLineOneCheck();
    SensorContextTester context = sensorContext(check);
    MapSettings mapSettings = context.settings();
    mapSettings.setProperty("sonar.text.analyzeAllFiles", true);
    context.setCancelled(true);
    analyse(sensor(check), context, inputFile("{}"));
    assertThat(context.allIssues()).isEmpty();
    assertThat(logTester.logs()).containsExactly(
      EXPECTED_PROCESSOR_LOG_LINE,
      DEFAULT_THREAD_USAGE_LOG_LINE,
      EXPECTED_SONAR_TEST_NOT_SET_LOG_LINE,
      "Start fetching files for the text and secrets analysis",
      "Retrieving all except non binary files",
      "Starting the text and secrets analysis",
      "1 source file to be analyzed for the text and secrets analysis");
  }

  @Test
  void analysisErrorShouldBeRaisedOnFailureInCheck() {
    @Rule(key = "Crash")
    class CrashCheck extends Check {
      protected String repositoryKey() {
        return SecretsRulesDefinition.REPOSITORY_KEY;
      }

      public void analyze(InputFileContext ctx) {
        throw new IllegalStateException("Crash");
      }
    }
    Check check = new CrashCheck();
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
  void specificationBasedCheckShouldBeInitializedAndReportErrorLog() {
    @Rule(key = "SecretKey")
    class SpecificationLoadingCheck extends SpecificationBasedCheck {
    }
    SpecificationLoadingCheck check = spy(new SpecificationLoadingCheck());
    SensorContextTester context = sensorContext(check);
    InputFile inputFile = inputFile("foo");
    analyse(sensor(check), context, inputFile);

    verify(check).initialize(any(), any(), any());
    assertThat(logTester.logs()).contains("Found no rule specification for rule with key: SecretKey");
  }

  @Rule(key = "Boom")
  static class BoomCheck extends TextCheck {
    public void analyze(InputFileContext ctx) {
      throw new IllegalStateException("Should not occurs because there are only binary files");
    }
  }

  @Test
  void shouldNotAnalyzeNonLanguageAssignedFilesInSonarQubeContext() {
    Check check = new ReportIssueAtLineOneCheck();
    SensorContextTester context = sensorContext(check);
    analyse(sensor(check), context, inputFile(Path.of("Foo.java"), SENSITIVE_BIDI_CHARS, null));
    assertCorrectLogsForTextAndSecretsAnalysis(0);
  }

  @Test
  void shouldAnalyzeLanguageAssignedFilesInSonarQubeContext() {
    Check check = new ReportIssueAtLineOneCheck();
    SensorContextTester context = sensorContext(check);
    analyse(sensor(check), context, inputFile(Path.of("Foo.java"), SENSITIVE_BIDI_CHARS, "java"));
    assertCorrectLogsForTextAndSecretsAnalysis(1);
  }

  @Test
  void shouldNotAnalyzeNonLanguageAssignedFilesInSonarQubeContextWhenPropertyIsSet() {
    Check check = new ReportIssueAtLineOneCheck();
    SensorContextTester context = sensorContext(check);
    context.setSettings(context.settings().setProperty("sonar.text.analyzeAllFiles", true));
    analyse(sensor(check), context, inputFile(Path.of("Foo.java"), SENSITIVE_BIDI_CHARS, null));
    assertCorrectLogsForTextAndSecretsAnalysis(0,
      "The file 'Foo.java' contains binary data and will not be included in the text and secrets analysis.",
      "Please check this file and/or remove the extension from the 'sonar.text.inclusions' property.");
  }

  @Test
  void shouldNotExecuteChecksOnBinaryFileNames() {
    Check check = new BoomCheck();
    SensorContextTester context = sensorContext(check);
    analyse(sensor(check), context, inputFile(Path.of("Foo.class"), "abc", null));

    // does not even contain "1/1 source file has been analyzed"
    assertCorrectLogsForTextAndSecretsAnalysis(0, false);
  }

  @Test
  void shouldExecuteChecksOnIncludedTextFileNames() {
    Check check = new ReportIssueAtLineOneCheck();
    SensorContextTester context = sensorContext(check);
    analyse(sensor(check), context, inputFile(Path.of("Foo.txt"), "abc", null));
    assertCorrectLogsForTextAndSecretsAnalysis(1);
  }

  @Test
  void shouldNotExecuteChecksOnNonIncludedTextFileNames() {
    Check check = new ReportIssueAtLineOneCheck();
    SensorContextTester context = sensorContext(check);
    analyse(sensor(check), context, inputFile(Path.of("Foo.csv"), "abc", null));
    assertCorrectLogsForTextAndSecretsAnalysis(0);
  }

  @Test
  void shouldExecuteChecksOnMultipleIncludedTextFileNames() {
    Check check = new ReportIssueAtLineOneCheck();
    SensorContextTester context = sensorContext(check);
    MapSettings mapSettings = context.settings();
    mapSettings.setProperty(TextAndSecretsSensor.TEXT_INCLUSIONS_KEY, "*.txt,*.csv");
    context.setSettings(mapSettings);
    analyse(sensor(check), context,
      inputFile(Path.of("Foo.txt"), SENSITIVE_BIDI_CHARS, null),
      inputFile(Path.of("Foo.csv"), SENSITIVE_BIDI_CHARS, null),
      inputFile(Path.of("Foo.nope"), "abc", null));
    assertCorrectLogsForTextAndSecretsAnalysis(0,
      "The file 'Foo.txt' contains binary data and will not be included in the text and secrets analysis.",
      "Please check this file and/or remove the extension from the 'sonar.text.inclusions' property.",
      "The file 'Foo.csv' contains binary data and will not be included in the text and secrets analysis.",
      "Please check this file and/or remove the extension from the 'sonar.text.inclusions' property.");
  }

  @Test
  void shouldNotExecuteChecksOnMultipleIncludedTextFileNamesWithoutAstrix() {
    Check check = new ReportIssueAtLineOneCheck();
    SensorContextTester context = sensorContext(check);
    MapSettings mapSettings = context.settings();
    mapSettings.setProperty(TextAndSecretsSensor.TEXT_INCLUSIONS_KEY, ".txt,.csv");
    context.setSettings(mapSettings);
    analyse(sensor(check), context,
      inputFile(Path.of("Foo.txt"), "abc", null),
      inputFile(Path.of("Foo.csv"), "abc", null),
      inputFile(Path.of("Foo.nope"), "abc", null));
    assertCorrectLogsForTextAndSecretsAnalysis(0);
  }

  @Test
  void shouldExecuteChecksOnDotEnvFile() {
    Check check = new ReportIssueAtLineOneCheck();
    SensorContextTester context = sensorContext(check);
    MapSettings mapSettings = context.settings();
    mapSettings.setProperty(TextAndSecretsSensor.TEXT_INCLUSIONS_KEY, ".env");
    context.setSettings(mapSettings);
    analyse(sensor(check), context,
      inputFile(Path.of(".env"), SENSITIVE_BIDI_CHARS, null),
      inputFile(Path.of("Foo.env"), SENSITIVE_BIDI_CHARS, null),
      inputFile(Path.of(".environment"), SENSITIVE_BIDI_CHARS, null),
      inputFile(Path.of("Foo.environment"), SENSITIVE_BIDI_CHARS, null));
    assertCorrectLogsForTextAndSecretsAnalysis(0,
      "The file '.env' contains binary data and will not be included in the text and secrets analysis.",
      "Please check this file and/or remove the extension from the 'sonar.text.inclusions' property.");
  }

  @Test
  void shouldExecuteChecksOnDotAwsConfig() {
    Check check = new ReportIssueAtLineOneCheck();
    SensorContextTester context = sensorContext(check);
    MapSettings mapSettings = context.settings();
    mapSettings.setProperty(TextAndSecretsSensor.TEXT_INCLUSIONS_KEY, ".aws/config");
    context.setSettings(mapSettings);
    analyse(sensor(check), context,
      inputFile(Path.of(".aws", "config"), SENSITIVE_BIDI_CHARS, null),
      inputFile(Path.of(".aws-config"), SENSITIVE_BIDI_CHARS, null),
      inputFile(Path.of(".aws/configuration"), SENSITIVE_BIDI_CHARS, null),
      inputFile(Path.of("config"), SENSITIVE_BIDI_CHARS, null));
    assertCorrectLogsForTextAndSecretsAnalysis(0,
      "The file '.aws/config' contains binary data and will not be included in the text and secrets analysis.",
      "Please check this file and/or remove the extension from the 'sonar.text.inclusions' property.");
  }

  @Test
  void shouldExecuteChecksOnDefaults() {
    Check check = new ReportIssueAtLineOneCheck();
    SensorContextTester context = sensorContext(check);
    MapSettings mapSettings = context.settings();
    // INCLUDED_FILE_SUFFIXES_KEY is set to default value
    mapSettings.setProperty(TextAndSecretsSensor.TEXT_INCLUSIONS_KEY, TEXT_INCLUSIONS_DEFAULT_VALUE);
    context.setSettings(mapSettings);
    analyse(sensor(check), context,
      inputFile(Path.of("script", "start.sh"), SENSITIVE_BIDI_CHARS, null),
      inputFile(Path.of("run.bash"), SENSITIVE_BIDI_CHARS, null),
      inputFile(Path.of("a.zsh"), SENSITIVE_BIDI_CHARS, null),
      inputFile(Path.of("b.ksh"), SENSITIVE_BIDI_CHARS, null),
      inputFile(Path.of("win.ps1"), SENSITIVE_BIDI_CHARS, null),
      inputFile(Path.of("gradle.properties"), SENSITIVE_BIDI_CHARS, null),
      inputFile(Path.of("config", "some.conf"), SENSITIVE_BIDI_CHARS, null),
      inputFile(Path.of("my.pem"), SENSITIVE_BIDI_CHARS, null),
      inputFile(Path.of("ccc.config"), SENSITIVE_BIDI_CHARS, null),
      inputFile(Path.of(".env"), SENSITIVE_BIDI_CHARS, null),
      inputFile(Path.of(".aws", "config"), SENSITIVE_BIDI_CHARS, null),
      // doesn't mach the pattern
      inputFile(Path.of("foo", "bar"), SENSITIVE_BIDI_CHARS, null));
    assertThat(logTester.logs()).contains(
      "Please check this file and/or remove the extension from the 'sonar.text.inclusions' property.",
      "The file 'config/some.conf' contains binary data and will not be included in the text and secrets analysis.",
      "The file '.aws/config' contains binary data and will not be included in the text and secrets analysis.",
      "The file 'script/start.sh' contains binary data and will not be included in the text and secrets analysis.",
      "The file 'my.pem' contains binary data and will not be included in the text and secrets analysis.",
      "The file '.env' contains binary data and will not be included in the text and secrets analysis.",
      "The file 'win.ps1' contains binary data and will not be included in the text and secrets analysis.",
      "The file 'run.bash' contains binary data and will not be included in the text and secrets analysis.",
      "The file 'ccc.config' contains binary data and will not be included in the text and secrets analysis.",
      "The file 'a.zsh' contains binary data and will not be included in the text and secrets analysis.",
      "The file 'b.ksh' contains binary data and will not be included in the text and secrets analysis.",
      "The file 'gradle.properties' contains binary data and will not be included in the text and secrets analysis.");
  }

  @Test
  void shouldAdviceUsersToRemoveTheFileFromSonarTextInclusionsIfThereIsNoLanguageAssigned() {
    Check check = new ReportIssueAtLineOneCheck();
    SensorContextTester context = sensorContext(check);
    analyse(sensor(check), context, inputFile(Path.of("Foo.txt"), SENSITIVE_BIDI_CHARS, null));
    assertCorrectLogsForTextAndSecretsAnalysis(0,
      "The file 'Foo.txt' contains binary data and will not be included in the text and secrets analysis.",
      "Please check this file and/or remove the extension from the 'sonar.text.inclusions' property.");
  }

  @Test
  void shouldExecuteChecksOnIncludedTextFileNamesWithBidiCharacters() {
    Check check = new ReportIssueAtLineOneCheck();
    SensorContextTester context = sensorContext(check);
    analyse(sensor(check), context, inputFile(Path.of("Foo.java"), SENSITIVE_BIDI_CHARS, "java"));
    assertCorrectLogsForTextAndSecretsAnalysis(1);
  }

  @Test
  void shouldNotExcludeBinaryFileContentIfLanguageIsNotNull() {
    Check check = new ReportIssueAtLineOneCheck();
    SensorContextTester context = testUtils().defaultSensorContext();
    analyse(sensor(check), context, inputFile(Path.of("Foo.java"), SENSITIVE_BIDI_CHARS, "java"));

    assertThat(asString(context.allIssues())).containsExactly(
      "text:IssueAtLineOne [1:0-1:2] testIssue");
    assertCorrectLogsForTextAndSecretsAnalysis(1);
  }

  @Test
  void shouldNotExcludeBinaryFileContentIfLanguageIsNullAndExtensionIncludedWithSonarqube() {
    Check check = new ReportIssueAtLineOneCheck();
    SensorContextTester context = testUtils().defaultSensorContext();
    analyse(sensor(check), context,
      inputFile(Path.of("Foo.txt"), SENSITIVE_BIDI_CHARS, null),
      inputFile(Path.of("FileWithoutExtension"), SENSITIVE_BIDI_CHARS, null));

    assertCorrectLogsForTextAndSecretsAnalysis(0,
      "The file 'Foo.txt' contains binary data and will not be included in the text and secrets analysis.",
      "Please check this file and/or remove the extension from the 'sonar.text.inclusions' property.");
  }

  @Test
  void shouldExcludeBinaryFileContentIfLanguageIsNullWithSonarlint() {
    Check check = new ReportIssueAtLineOneCheck();
    SensorContextTester context = testUtils().defaultSensorContext();
    context.setRuntime(SONARLINT_RUNTIME);
    analyse(sensor(check), context,
      inputFile(Path.of("Foo.txt"), SENSITIVE_BIDI_CHARS, null),
      inputFile(Path.of("FileWithoutExtension"), SENSITIVE_BIDI_CHARS, null));

    assertThat(asString(context.allIssues())).isEmpty();
    assertCorrectLogsForTextAndSecretsAnalysis(0, false,
      "The file 'Foo.txt' contains binary data and will not be included in the text and secrets analysis.");
  }

  @Test
  public void shouldNotExcludeBinaryFileExtensionDynamically() throws IOException {
    Check check = new BoomCheck();
    SensorContextTester context = sensorContext(check);
    MapSettings mapSettings = context.settings();
    mapSettings.setProperty("sonar.text.analyzeAllFiles", true);
    analyseDirectory(sensor(check), context, Path.of("src", "test", "resources", "binary-files"));
    assertCorrectLogsForTextAndSecretsAnalysis(0, false);

    assertThat(logTester.logs())
      .contains("The file 'src/test/resources/binary-files/Foo.unknown1' contains binary data and will not be included in the text and secrets analysis.")
      .contains("The file 'src/test/resources/binary-files/Foo.unknown2' contains binary data and will not be included in the text and secrets analysis.")
      .contains("The file 'src/test/resources/binary-files/Bar.unknown1' contains binary data and will not be included in the text and secrets analysis.");

  }

  @Test
  void analysisErrorShouldBeRaisedOnCorruptedFile() throws IOException {
    Check check = new BIDICharacterCheck();
    SensorContextTester context = sensorContext(check);
    InputFile inputFile = spy(inputFile(Path.of("a.txt"), "{}", "secrets"));
    when(inputFile.inputStream()).thenThrow(new IOException("Fail to read file input stream"));
    analyse(sensor(check), context, inputFile);

    Collection<AnalysisError> analysisErrors = context.allAnalysisErrors();
    assertThat(analysisErrors).hasSize(1);
    AnalysisError analysisError = analysisErrors.iterator().next();
    assertThat(analysisError.inputFile()).isEqualTo(inputFile);
    assertThat(analysisError.message()).startsWith("Unable to analyze").endsWith("Fail to read file input stream");
    assertThat(analysisError.location()).isNull();
    // It looks like org.eclipse.jgit.util.FS_POSIX call LOG.warn(null) or similar
    assertThat(logTester.logs()).anyMatch(log -> log != null && log.startsWith("Unable to analyze"));
  }

  @Test
  void shouldNotAnalyzeUntrackedFilesNotBelongingToALanguage() {
    Check check = new ReportIssueAtLineOneCheck();
    SensorContextTester context = sensorContext(check);
    var sensor = sensor(check);
    var sensorSpy = spy(sensor);
    String relativePathFooJava = "src" + File.pathSeparator + "foo.java";
    var gitService = mock(GitService.class);
    when(gitService.retrieveUntrackedFileNames())
      .thenReturn(new GitService.UntrackedFileNamesResult(true, Set.of("c.txt", "d.txt", relativePathFooJava)));
    when(sensorSpy.createGitService(any())).thenReturn(gitService);

    analyse(sensorSpy, context,
      // tracked files
      inputFile(Path.of("a.txt"), "{}", "secrets"),
      inputFile(Path.of("b.txt"), "{}"),

      // untracked files
      inputFile(Path.of("c.txt"), "{}", "secrets"),
      inputFile(Path.of("d.txt"), "{}"),
      // no language assigned to file and not part of the included path patterns
      inputFile(Path.of("src", "foo.java"), "{}"));

    Collection<Issue> issues = context.allIssues();
    assertThat(issues)
      .hasSize(3)
      .map(it -> ((InputFile) it.primaryLocation().inputComponent()).filename())
      .containsExactlyInAnyOrder("a.txt", "b.txt", "c.txt");
    assertCorrectLogsForTextAndSecretsAnalysis(3, "1 file is ignored because it is untracked by git");
  }

  static Set<SonarRuntime> shouldNotLeakThreads() {
    return TestUtils.sonarRuntimes();
  }

  @ParameterizedTest
  @MethodSource
  void shouldNotLeakThreads(SonarRuntime sonarRuntime) {
    Check check = new ReportIssueAtLineOneCheck();
    SensorContextTester context = sensorContext(check);
    context.setRuntime(sonarRuntime);
    var sensor = sensor(check);

    var threadsBefore = activeCreatedThreadsNames();
    analyse(sensor, context,
      inputFile(Path.of("a.txt"), "{}", "secrets"),
      inputFile(Path.of("b.txt"), "{}", "secrets"),
      inputFileFromPath(Path.of("src", "foo.java")));

    Callable<Boolean> noThreadsAreStillRunning = () -> {
      var threadsAfter = activeCreatedThreadsNames();
      threadsAfter.removeAll(threadsBefore);
      return threadsAfter.isEmpty();
    };

    await().atMost(Duration.of(2, ChronoUnit.SECONDS)).until(noThreadsAreStillRunning);
    try {
      assertThat(noThreadsAreStillRunning.call()).isTrue();
    } catch (Exception e) {
      fail("Unexpected exception occurred: " + e.getMessage(), e);
    }
  }

  static List<String> activeCreatedThreadsNames() {
    var result = new ArrayList<String>();
    ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
    for (ThreadInfo threadInfo : threadMXBean.dumpAllThreads(true, true)) {
      // the demon threads don't block the application termination
      // the "awaitility-thread" threads are created by the Awaitility test library
      if (!threadInfo.isDaemon() && !threadInfo.getThreadName().equals("awaitility-thread")) {
        result.add(threadInfo.getThreadName());
      }
    }
    return result;
  }

  @Test
  void shouldNotCallGitFilePredicateInSonarlintContext() {
    Check check = new ReportIssueAtLineOneCheck();
    SensorContextTester context = sensorContext(check);
    context.setRuntime(SONARLINT_RUNTIME);
    var sensor = sensor(check);
    var sensorSpy = spy(sensor);
    var gitService = mock(GitService.class);
    when(sensorSpy.createGitService(any())).thenReturn(gitService);

    analyse(sensorSpy, context,
      inputFile(Path.of("a.txt"), "{}"),
      inputFile(Path.of("b.txt"), "{}"));

    Collection<Issue> issues = context.allIssues();
    assertThat(issues).hasSize(2);
    assertCorrectLogsForTextAndSecretsAnalysis(2, false);
    verify(gitService, times(0)).retrieveUntrackedFileNames();
  }

  @Test
  void shouldCallGitFilePredicateOnDefault() {
    logTester.setLevel(Level.DEBUG);
    Check check = new ReportIssueAtLineOneCheck();
    SensorContextTester context = sensorContext(check);
    var sensor = sensor(check);
    var sensorSpy = spy(sensor);
    var gitService = mock(GitService.class);
    when(gitService.retrieveUntrackedFileNames())
      .thenReturn(new GitService.UntrackedFileNamesResult(true, Set.of("a.txt", "c.txt", "d.txt")));
    when(sensorSpy.createGitService(any())).thenReturn(gitService);

    analyse(sensorSpy, context,
      inputFile(Path.of("a.txt"), "{}"),
      inputFile(Path.of("b.txt"), "{}"),
      inputFile(Path.of("c.txt"), "{}"),
      inputFile(Path.of("d.txt"), "{}"));

    Collection<Issue> issues = context.allIssues();
    assertThat(issues).hasSize(1);
    assertCorrectLogsForTextAndSecretsAnalysis(1,
      "Retrieving language associated files and files included via \"sonar.text.inclusions\" that are tracked by git",
      "3 files are ignored because they are untracked by git",
      """
        Files untracked by git:
        \ta.txt
        \tc.txt
        \td.txt""");
  }

  @Test
  void shouldNotCallGitFilePredicate() {
    logTester.setLevel(Level.DEBUG);
    Check check = new ReportIssueAtLineOneCheck();
    SensorContextTester context = sensorContext(check);
    MapSettings settings = context.settings();
    settings.setProperty(TextAndSecretsSensor.INCLUSIONS_ACTIVATION_KEY, "false");
    var sensor = sensor(check);
    var sensorSpy = spy(sensor);
    var gitService = mock(GitService.class);
    when(gitService.retrieveUntrackedFileNames()).thenReturn(new GitService.UntrackedFileNamesResult(true, Set.of("a.txt")));
    when(sensorSpy.createGitService(any())).thenReturn(gitService);

    analyse(sensorSpy, context,
      inputFile(Path.of("a.txt"), "{}", "secrets"),
      inputFile(Path.of("b.txt"), "{}", "secrets"));

    Collection<Issue> issues = context.allIssues();
    assertThat(issues).hasSize(2);
    assertCorrectLogsForTextAndSecretsAnalysis(2,
      "Retrieving only language associated files, \"sonar.text.inclusions.activate\" property is deactivated");
    verify(gitService, times(0)).retrieveUntrackedFileNames();
  }

  @Test
  void shouldOnlyAnalyzeFilesBelongingToALanguageNoGitRepositoryIsFoundEvenIfInclusionsKeySet() {
    logTester.setLevel(Level.DEBUG);
    Check check = new ReportIssueAtLineOneCheck();
    SensorContextTester context = sensorContext(check);
    var sensor = sensor(check);
    var sensorSpy = spy(sensor);
    var gitService = mock(GitService.class);
    when(gitService.retrieveUntrackedFileNames()).thenReturn(new GitService.UntrackedFileNamesResult(false, Set.of()));
    when(sensorSpy.createGitService(any())).thenReturn(gitService);

    analyse(sensorSpy, context,
      inputFile(Path.of("a.txt"), "{}", "secrets"),
      inputFile(Path.of("b.txt"), "{}"));

    Collection<Issue> issues = context.allIssues();
    assertThat(issues).hasSize(1);
    assertCorrectLogsForTextAndSecretsAnalysis(1,
      "Retrieving only language associated files, make sure to run the analysis " +
        "inside a git repository to make use of inclusions specified via \"sonar.text.inclusions\"");
  }

  @Test
  void shouldUsePropertyDefinedTimeoutValues() {
    Check check = new ReportIssueAtLineOneCheck();
    SensorContextTester context = sensorContext(check);
    MapSettings mapSettings = context.settings();
    mapSettings.setProperty(TextAndSecretsSensor.REGEX_MATCH_TIMEOUT_KEY, "1");
    mapSettings.setProperty(TextAndSecretsSensor.REGEX_EXECUTION_TIMEOUT_KEY, "2");
    context.setSettings(mapSettings);
    analyse(sensor(check), context,
      inputFile(Path.of(".txt"), "{}"));
    assertThat(RegexMatchingManager.getTimeoutMs()).isEqualTo(1);
    assertThat(RegexMatchingManager.getUninterruptibleTimeoutMs()).isEqualTo(2);
  }

  @ParameterizedTest
  @ValueSource(strings = {
    "-1",
    "4.5",
    "0",
    "no-number",
    "null"
  })
  void shouldUseDefaultTimeoutWhenPropertyNotInAValidFormat(String propertyValue) {
    int defaultTimeout = 10_000;
    Check check = new ReportIssueAtLineOneCheck();
    SensorContextTester context = sensorContext(check);
    MapSettings mapSettings = context.settings();
    mapSettings.setProperty(TextAndSecretsSensor.REGEX_MATCH_TIMEOUT_KEY, propertyValue);
    mapSettings.setProperty(TextAndSecretsSensor.REGEX_EXECUTION_TIMEOUT_KEY, propertyValue);
    context.setSettings(mapSettings);
    // actual behavior to test
    analyse(sensor(check), context,
      inputFile(Path.of(".txt"), "{}"));
    assertThat(RegexMatchingManager.getTimeoutMs()).isEqualTo(defaultTimeout);
    assertThat(RegexMatchingManager.getUninterruptibleTimeoutMs()).isEqualTo(defaultTimeout);
  }

  @Test
  void shouldNotStartAnalysisWhenAnalysisIsDeactivated() {
    Check check = new ReportIssueAtLineOneCheck();
    InputFile inputFile = inputFile("foo");

    SensorContextTester context = sensorContext(check);
    MapSettings mapSettings = context.settings();
    mapSettings.setProperty(TextAndSecretsSensor.ANALYZER_ACTIVATION_KEY, "false");
    context.setSettings(mapSettings);
    analyse(sensor(check), context, inputFile);

    assertThat(context.allIssues()).isEmpty();
    assertThat(logTester.logs()).isEmpty();
  }

  @Test
  void shouldExecuteAnalysisWithOneThread() {
    Check check = new ReportIssueAtLineOneCheck();
    SensorContextTester context = sensorContext(check);
    MapSettings mapSettings = context.settings();
    mapSettings.setProperty(TextAndSecretsSensor.THREAD_NUMBER_KEY, "1");
    context.setSettings(mapSettings);
    analyse(sensor(check), context,
      inputFile(Path.of("a.txt"), "foo", "secrets"),
      inputFile(Path.of("b.java"), "bar", "java"),
      inputFile(Path.of("c.c"), "abc", "c"));
    assertThat(logTester.logs()).containsExactlyInAnyOrder(
      EXPECTED_SONAR_TEST_NOT_SET_LOG_LINE,
      "Available processors: " + Runtime.getRuntime().availableProcessors(),
      "Using 1 thread for analysis, according to the value of \"sonar.text.threads\" property.",
      "Start fetching files for the text and secrets analysis",
      "Starting the text and secrets analysis",
      "Using Git CLI to retrieve untracked files",
      "Retrieving language associated files and files included via \"sonar.text.inclusions\" that are tracked by git",
      "3 source files to be analyzed for the text and secrets analysis",
      "3/3 source files have been analyzed for the text and secrets analysis");
  }

  @Test
  void shouldExecuteAnalysisWithMoreThreadsThanAvailableProcessors() {
    int availableProcessors = Runtime.getRuntime().availableProcessors();
    String usedThreads = String.valueOf(availableProcessors + 1);
    Check check = new ReportIssueAtLineOneCheck();
    SensorContextTester context = sensorContext(check);
    MapSettings mapSettings = context.settings();
    mapSettings.setProperty(TextAndSecretsSensor.THREAD_NUMBER_KEY, usedThreads);
    context.setSettings(mapSettings);
    analyse(sensor(check), context,
      inputFile(Path.of("a.txt"), "foo", "secrets"),
      inputFile(Path.of("b.java"), "bar", "java"),
      inputFile(Path.of("c.c"), "abc", "c"));
    assertThat(logTester.logs()).containsExactlyInAnyOrder(
      EXPECTED_SONAR_TEST_NOT_SET_LOG_LINE,
      "Available processors: " + availableProcessors,
      "Using " + usedThreads + " threads for analysis, according to the value of \"sonar.text.threads\" property.",
      "\"sonar.text.threads\" property was set to " + usedThreads + ", which is greater than the number of available processors: " + availableProcessors + ".\n" +
        "It is recommended to let the analyzer detect the number of threads automatically by not setting the property.\n" +
        "For more information, visit the documentation page.",
      "Start fetching files for the text and secrets analysis",
      "Starting the text and secrets analysis",
      "Using Git CLI to retrieve untracked files",
      "Retrieving language associated files and files included via \"sonar.text.inclusions\" that are tracked by git",
      "3 source files to be analyzed for the text and secrets analysis",
      "3/3 source files have been analyzed for the text and secrets analysis");
  }

  @Test
  void shouldExecuteAnalysisInSonarCloudRuntime() {
    int availableProcessors = Runtime.getRuntime().availableProcessors();
    Check check = new ReportIssueAtLineOneCheck();
    SensorContextTester context = sensorContext(check);
    MapSettings mapSettings = context.settings();
    mapSettings.setProperty(TextAndSecretsSensor.THREAD_NUMBER_KEY, String.valueOf(availableProcessors + 1));
    context.setSettings(mapSettings);
    context.setRuntime(SONARCLOUD_RUNTIME);
    analyse(sensor(check), context,
      inputFile(Path.of("a.txt"), "foo", "secrets"),
      inputFile(Path.of("b.java"), "bar", "java"),
      inputFile(Path.of("c.c"), "abc", "c"));
    assertThat(logTester.logs()).containsExactlyInAnyOrder(
      "Available processors: " + availableProcessors,
      "Using " + availableProcessors + " threads for analysis, \"sonar.text.threads\" is ignored.",
      "Start fetching files for the text and secrets analysis",
      "Starting the text and secrets analysis",
      "Using Git CLI to retrieve untracked files",
      "Retrieving language associated files and files included via \"sonar.text.inclusions\" that are tracked by git",
      "3 source files to be analyzed for the text and secrets analysis",
      "3/3 source files have been analyzed for the text and secrets analysis",
      EXPECTED_SONAR_TEST_NOT_SET_LOG_LINE);
  }

  @Test
  void shouldNotLogMessageWhenSonarTestIsNotSetWithSonarlint() {
    SensorContextTester context = testUtils().defaultSensorContext();
    context.setRuntime(SONARLINT_RUNTIME);
    var settings = context.settings().setProperty(SONAR_TESTS_KEY, "");
    context.setSettings(settings);
    analyse(sensor(context), context, inputFile(""));
    assertThat(logTester.logs(Level.INFO)).doesNotContain(EXPECTED_SONAR_TEST_NOT_SET_LOG_LINE);
  }

  @Test
  void shouldLogMessageWhenSonarTestIsNotSetWithSonarqube() {
    SensorContextTester context = testUtils().sonarqubeSensorContext();
    var settings = context.settings().setProperty(SONAR_TESTS_KEY, "");
    context.setSettings(settings);
    analyse(sensor(context), context, inputFile(""));
    assertThat(logTester.logs(Level.INFO)).contains(EXPECTED_SONAR_TEST_NOT_SET_LOG_LINE);
  }

  @Test
  void shouldNotLogMessageWhenSonarTestIsSet() {
    SensorContextTester context = testUtils().defaultSensorContext();
    var settings = context.settings().setProperty(SONAR_TESTS_KEY, "src/test");
    context.setSettings(settings);
    analyse(sensor(context), context, inputFile(""));
    assertThat(logTester.logs(Level.INFO)).doesNotContain(EXPECTED_SONAR_TEST_NOT_SET_LOG_LINE);
  }

  @Test
  void numberOfSecretChecksShouldBeIdenticalToLoadedSecretSpecificationRSPECKeys() {
    var sensor = sensor(testUtils().defaultSensorContext());
    SecretsSpecificationLoader secretsSpecificationLoader = sensor.constructSpecificationLoader();
    int numberOfSecretSpecificationRSPECKeys = secretsSpecificationLoader.getRulesMappedToKey().size();

    // TemplateRule is an interface that is not implemented by the secret checks that are loaded from the secret specification
    var numberOfSecretChecksWithoutTemplateRules = testUtils().secretCheckClassList().stream()
      .filter(check -> Arrays.stream(check.getInterfaces())
        .noneMatch(i -> i.getName().contains("TemplateRule")))
      .filter(check -> !AbstractBinaryFileCheck.class.isAssignableFrom(check))
      .count();

    assertThat(numberOfSecretSpecificationRSPECKeys).isEqualTo(numberOfSecretChecksWithoutTemplateRules);
  }

  @Test
  void numberOfActiveChecksShouldMatchNumberOfChecksInCheckLists() {
    var sensor = sensor(testUtils().defaultSensorContext());
    var numberOfActiveChecks = sensor.getActiveChecks().size();
    var numberOfChecksInCheckLists = testUtils().secretCheckClassList().size() + testUtils().textCheckClassList().size();

    assertThat(numberOfActiveChecks).isEqualTo(numberOfChecksInCheckLists);
  }

  @Rule(key = "BinaryCheck")
  public static class TestBinaryFileCheck extends AbstractBinaryFileCheck {
    public void analyze(InputFileContext ctx) {
      ctx.reportIssueOnFile(getRuleKey(), "binaryIssue");
    }
  }

  @Test
  void shouldAnalyzeKeystoreAndJksFilesInBinaryFileAnalysis() {
    Check binaryFileCheck = new TestBinaryFileCheck();
    SensorContextTester context = sensorContext(binaryFileCheck);
    var sensor = sensor(binaryFileCheck);

    analyse(sensor, context,
      inputFile(Path.of("a.jks"), SENSITIVE_BIDI_CHARS),
      inputFile(Path.of("b.keystore"), SENSITIVE_BIDI_CHARS),
      inputFileFromPath(Path.of("src", "test", "resources", "keystoreFiles", "myKeystoreFile.jks")));

    var expectedIssuesSize = 0;
    var expectedLogs = List.of(
      EXPECTED_PROCESSOR_LOG_LINE,
      DEFAULT_THREAD_USAGE_LOG_LINE,
      EXPECTED_SONAR_TEST_NOT_SET_LOG_LINE);

    if (!isPublicSensor()) {
      expectedIssuesSize = 3;

      expectedLogs = new ArrayList<>(expectedLogs);
      expectedLogs.addAll(List.of(
        "Start fetching files for the binary file analysis",
        "Starting the binary file analysis",
        "3 source files to be analyzed for the binary file analysis",
        "3/3 source files have been analyzed for the binary file analysis"));
    }

    assertThat(asString(context.allIssues())).hasSize(expectedIssuesSize);
    assertThat(logTester.logs()).containsExactly(expectedLogs.toArray(new String[0]));
  }

  @Test
  void retrievalOfBinaryFilesShouldNotBeInfluencedByFailedGitStatusCall() {
    Check binaryFileCheck = new TestBinaryFileCheck();
    SensorContextTester context = sensorContext(binaryFileCheck);
    var sensorSpy = spy(sensor(binaryFileCheck));
    var gitService = mock(GitService.class);
    when(gitService.retrieveUntrackedFileNames()).thenReturn(new GitService.UntrackedFileNamesResult(false, Set.of()));
    when(sensorSpy.createGitService(any())).thenReturn(gitService);

    analyse(sensorSpy, context,
      inputFile(Path.of("a.jks"), SENSITIVE_BIDI_CHARS),
      inputFile(Path.of("b.keystore"), SENSITIVE_BIDI_CHARS));

    var expectedIssuesSize = 0;
    var expectedLogs = List.of(
      EXPECTED_PROCESSOR_LOG_LINE,
      DEFAULT_THREAD_USAGE_LOG_LINE,
      EXPECTED_SONAR_TEST_NOT_SET_LOG_LINE);

    if (!isPublicSensor()) {
      expectedIssuesSize = 2;

      expectedLogs = new ArrayList<>(expectedLogs);
      expectedLogs.addAll(List.of(
        "Start fetching files for the binary file analysis",
        "Starting the binary file analysis",
        "2 source files to be analyzed for the binary file analysis",
        "2/2 source files have been analyzed for the binary file analysis"));
    }

    assertThat(asString(context.allIssues())).hasSize(expectedIssuesSize);
    assertThat(logTester.logs()).containsExactly(expectedLogs.toArray(new String[0]));
  }

  @Test
  void shouldNotFilterOutGitUntrackedKeystoreAndJksFilesInBinaryFileAnalysis() {
    Check binaryFileCheck = new TestBinaryFileCheck();
    SensorContextTester context = sensorContext(binaryFileCheck);
    var sensorSpy = spy(sensor(binaryFileCheck));
    var gitService = mock(GitService.class);
    when(gitService.retrieveUntrackedFileNames()).thenReturn(new GitService.UntrackedFileNamesResult(true, Set.of("a.jks", "b.keystore")));
    when(sensorSpy.createGitService(any())).thenReturn(gitService);

    analyse(sensorSpy, context,
      // untracked files
      inputFile(Path.of("a.jks"), SENSITIVE_BIDI_CHARS),
      inputFile(Path.of("b.keystore"), SENSITIVE_BIDI_CHARS));

    var expectedIssuesSize = 0;
    var expectedLogs = List.of(
      EXPECTED_PROCESSOR_LOG_LINE,
      DEFAULT_THREAD_USAGE_LOG_LINE,
      EXPECTED_SONAR_TEST_NOT_SET_LOG_LINE);

    if (!isPublicSensor()) {
      expectedIssuesSize = 2;

      expectedLogs = new ArrayList<>(expectedLogs);
      expectedLogs.addAll(List.of(
        "Start fetching files for the binary file analysis",
        "Starting the binary file analysis",
        "2 source files to be analyzed for the binary file analysis",
        "2/2 source files have been analyzed for the binary file analysis"));
    }

    assertThat(asString(context.allIssues())).hasSize(expectedIssuesSize);
    assertThat(logTester.logs()).containsExactly(expectedLogs.toArray(new String[0]));
  }

  @Test
  void shouldAnalyzeTheCorrectAmountOfFilesPerAnalyzer() {
    Check binaryFileCheck = new TestBinaryFileCheck();
    Check reportIssueAtLineOneCheck = new ReportIssueAtLineOneCheck();
    SensorContextTester context = sensorContext(binaryFileCheck, reportIssueAtLineOneCheck);
    var sensor = sensor(binaryFileCheck, reportIssueAtLineOneCheck);

    analyse(sensor, context,
      inputFile(Path.of("a.jks"), SENSITIVE_BIDI_CHARS),
      inputFile(Path.of("b.keystore"), SENSITIVE_BIDI_CHARS),
      inputFile(Path.of("c.java"), "{}", "java"),
      inputFile(Path.of("d.txt"), "{}"));

    var expectedIssues = List.of(
      "text:IssueAtLineOne [1:0-1:2] testIssue",
      "text:IssueAtLineOne [1:0-1:2] testIssue");
    var expectedLogs = List.of(
      EXPECTED_PROCESSOR_LOG_LINE,
      DEFAULT_THREAD_USAGE_LOG_LINE,
      EXPECTED_SONAR_TEST_NOT_SET_LOG_LINE,
      "Start fetching files for the text and secrets analysis",
      "Using Git CLI to retrieve untracked files",
      "Retrieving language associated files and files included via \"sonar.text.inclusions\" that are tracked by git",
      "Starting the text and secrets analysis",
      "2 source files to be analyzed for the text and secrets analysis",
      "2/2 source files have been analyzed for the text and secrets analysis");

    if (!isPublicSensor()) {
      expectedIssues = new ArrayList<>(expectedIssues);
      expectedIssues.add("secrets:BinaryCheck [] binaryIssue");
      expectedIssues.add("secrets:BinaryCheck [] binaryIssue");

      expectedLogs = new ArrayList<>(expectedLogs);
      expectedLogs.addAll(List.of(
        "Start fetching files for the binary file analysis",
        "Starting the binary file analysis",
        "2 source files to be analyzed for the binary file analysis",
        "2/2 source files have been analyzed for the binary file analysis"));
    }

    assertThat(asString(context.allIssues())).containsExactly(expectedIssues.toArray(new String[0]));
    assertThat(logTester.logs()).containsExactly(expectedLogs.toArray(new String[0]));
  }

  @Test
  void shouldInitializeBinaryFileCheck() {
    AbstractBinaryFileCheck binaryFileCheck = spy(new TestBinaryFileCheck());
    SensorContextTester context = sensorContext(binaryFileCheck);
    var sensor = sensor(binaryFileCheck);

    analyse(sensor, context,
      inputFile(Path.of("a.jks"), SENSITIVE_BIDI_CHARS));

    var wantedInitialization = 0;

    if (!isPublicSensor()) {
      wantedInitialization = 1;
    }

    verify(binaryFileCheck, times(wantedInitialization)).initialize(any(), any());
  }

  @Test
  void shouldAnalyzeAllTrackedHiddenFiles() {
    Check binaryFileCheck = new TestBinaryFileCheck();
    Check reportIssueAtLineOneCheck = new ReportIssueAtLineOneCheck();
    SensorContextTester context = sensorContext(binaryFileCheck, reportIssueAtLineOneCheck);

    var sensor = spy(sensor(binaryFileCheck, reportIssueAtLineOneCheck));
    var gitService = mock(GitService.class);
    when(gitService.retrieveUntrackedFileNames())
      .thenReturn(new GitService.UntrackedFileNamesResult(true, Set.of(".untracked", ".hidden" + File.pathSeparator + "untracked.jks")));
    when(sensor.createGitService(any())).thenReturn(gitService);

    var hiddenFile = hiddenInputFile(Path.of(".a"), "{}");
    var hiddenDirectoryFile = hiddenInputFile(Path.of(".hidden", "b.txt"), "{}");
    var untrackedHiddenFile = hiddenInputFile(Path.of(".untracked"), "{}");
    var hiddenBinaryFile = hiddenInputFile(Path.of(".keystore"), SENSITIVE_BIDI_CHARS);
    var untrackedHiddenBinaryFile = hiddenInputFile(Path.of(".hidden", "untracked.jks"), SENSITIVE_BIDI_CHARS);

    analyse(sensor, context, hiddenFile, untrackedHiddenFile, hiddenDirectoryFile, hiddenBinaryFile, untrackedHiddenBinaryFile);

    var expectedIssues = List.of(
      "text:IssueAtLineOne [1:0-1:2] testIssue",
      "text:IssueAtLineOne [1:0-1:2] testIssue");

    if (!isPublicSensor()) {
      expectedIssues = new ArrayList<>(expectedIssues);
      expectedIssues.add("secrets:BinaryCheck [] binaryIssue");
      // Binary files are analyzed based on extensions and do not have the file inclusions logic
      // As long as the hidden keystore file is picked up by the sensor, it will be analyzed by the binary analyzer
      // So ".hidden/untracked.jks" is analyzed
      expectedIssues.add("secrets:BinaryCheck [] binaryIssue");
    }

    assertThat(asString(context.allIssues())).containsExactly(expectedIssues.toArray(new String[0]));
    assertCorrectLogsForTextAndSecretsAnalysis(4);
  }

  @Test
  void shouldNotAnalyzeTrackedHiddenBinaryFilesInTextAndSecretsAnalysis() {
    Check reportIssueAtLineOneCheck = new ReportIssueAtLineOneCheck();
    SensorContextTester context = sensorContext(reportIssueAtLineOneCheck);

    var sensor = spy(sensor(reportIssueAtLineOneCheck));
    var gitService = mock(GitService.class);
    when(gitService.retrieveUntrackedFileNames())
      .thenReturn(new GitService.UntrackedFileNamesResult(true, Set.of()));
    when(sensor.createGitService(any())).thenReturn(gitService);

    var hiddenBinaryFile = hiddenInputFile(Path.of(".keystore"), SENSITIVE_BIDI_CHARS);

    analyse(sensor, context, hiddenBinaryFile);

    assertThat(asString(context.allIssues())).isEmpty();
    assertCorrectLogsForTextAndSecretsAnalysis(0);
  }

  @Test
  void shouldNotAnalyzeTrackedHiddenFilesWhenRuntimeDoesNotSupportIt() {
    Check binaryFileCheck = new TestBinaryFileCheck();
    Check reportIssueAtLineOneCheck = new ReportIssueAtLineOneCheck();
    SensorContextTester context = sensorContext(binaryFileCheck, reportIssueAtLineOneCheck);
    context.setRuntime(SONARQUBE_RUNTIME_WITHOUT_HIDDEN_FILES_SUPPORT);

    var sensor = spy(sensor(binaryFileCheck, reportIssueAtLineOneCheck));
    var gitService = mock(GitService.class);
    when(gitService.retrieveUntrackedFileNames())
      .thenReturn(new GitService.UntrackedFileNamesResult(true, Set.of()));
    when(sensor.createGitService(any())).thenReturn(gitService);

    var hiddenFile = hiddenInputFile(Path.of(".a"), "{}");
    var hiddenDirectoryFile = hiddenInputFile(Path.of(".hidden", "b.txt"), "{}");
    var hiddenBinaryFile = hiddenInputFile(Path.of(".keystore"), SENSITIVE_BIDI_CHARS);

    analyse(sensor, context, hiddenFile, hiddenDirectoryFile, hiddenBinaryFile);

    // Binary files are analyzed based on extensions and do not have the file inclusions logic
    // As long as the hidden keystore file is picked up by the sensor, it will be analyzed by the binary analyzer
    List<String> expectedIssues = isPublicSensor() ? List.of() : List.of("secrets:BinaryCheck [] binaryIssue");

    assertThat(asString(context.allIssues())).contains(expectedIssues.toArray(new String[0]));
    verify(hiddenFile, never()).isHidden();
    verify(hiddenDirectoryFile, never()).isHidden();
    verify(hiddenBinaryFile, never()).isHidden();
  }

  @Test
  void shouldSendTelemetryWhenRuntimeSupportsIt() {
    var check = new ReportIssueAtLineOneCheck();
    var binaryCheck = new TestBinaryFileCheck();
    var context = spy(sensorContext(check, binaryCheck));
    var sensor = sensor(check, binaryCheck);

    analyse(sensor, context,
      inputFile(Path.of("a.txt"), "{}", "secrets"),
      inputFile(Path.of("android.keystore"), SENSITIVE_BIDI_CHARS),
      inputFile(Path.of("a.bc"), "{}"),
      hiddenInputFile(Path.of(".env"), "{}", "secrets"),
      hiddenInputFile(Path.of(".keystore"), SENSITIVE_BIDI_CHARS));

    var analysisTimeMeasureKey = TelemetryReporter.KEY_PREFIX + "sensor_time_ms_" + sensor.getEditionName().toLowerCase(Locale.ROOT);
    var fileMeasureKey = TelemetryReporter.KEY_PREFIX + Analyzer.ANALYZED_FILES_MEASURE_KEY;
    var hiddenFileMeasureKey = TelemetryReporter.KEY_PREFIX + Analyzer.ANALYZED_HIDDEN_FILES_MEASURE_KEY;
    var allTrackedTextFilesMeasureKey = TelemetryReporter.KEY_PREFIX + TextAndSecretsSensor.ALL_TRACKED_TEXT_FILES_MEASURE_KEY;
    var expectedFilesCount = isPublicSensor() ? "2" : "4";
    var expectedHiddenFilesCount = isPublicSensor() ? "1" : "2";
    verify(context).addTelemetryProperty(eq(analysisTimeMeasureKey), argThat(value -> Integer.parseInt(value) > 0));
    verify(context).addTelemetryProperty(fileMeasureKey, expectedFilesCount);
    verify(context).addTelemetryProperty(hiddenFileMeasureKey, expectedHiddenFilesCount);
    verify(context).addTelemetryProperty(allTrackedTextFilesMeasureKey, "3");
  }

  @Test
  void shouldSendFileTelemetryWhenRuntimeSupportsItAndThereIsNoHiddenFile() {
    var check = new ReportIssueAtLineOneCheck();
    var binaryCheck = new TestBinaryFileCheck();
    var context = spy(sensorContext(check, binaryCheck));
    var sensor = sensor(check, binaryCheck);

    analyse(sensor, context, inputFile(Path.of("a.txt"), "{}", "secrets"));

    var fileMeasureKey = TelemetryReporter.KEY_PREFIX + Analyzer.ANALYZED_FILES_MEASURE_KEY;
    var hiddenFileMeasureKey = TelemetryReporter.KEY_PREFIX + Analyzer.ANALYZED_HIDDEN_FILES_MEASURE_KEY;
    verify(context).addTelemetryProperty(fileMeasureKey, "1");
    verify(context).addTelemetryProperty(hiddenFileMeasureKey, "0");
  }

  @Test
  void shouldNotSendTelemetryWhenRuntimeDoesNotSupportTelemetry() {
    var check = new ReportIssueAtLineOneCheck();
    var binaryCheck = new TestBinaryFileCheck();
    var context = spy(sensorContext(check, binaryCheck));
    context.setRuntime(SONARQUBE_RUNTIME_WITHOUT_TELEMETRY_SUPPORT);
    var sensor = sensor(check, binaryCheck);

    var hiddenFile = hiddenInputFile(Path.of(".env"), "{}", "secrets");
    var hiddenBinaryFile = hiddenInputFile(Path.of(".keystore"), SENSITIVE_BIDI_CHARS);
    analyse(sensor, context,
      inputFile(Path.of("a.txt"), "{}", "secrets"),
      hiddenFile,
      hiddenBinaryFile);

    verify(context, never()).addTelemetryProperty(any(), any());
  }

  @Test
  void shouldOnlySendRegularFileTelemetryWhenRuntimeDoesNotSupportHiddenFiles() {
    var check = new ReportIssueAtLineOneCheck();
    var binaryCheck = new TestBinaryFileCheck();
    var context = spy(sensorContext(check, binaryCheck));
    context.setRuntime(SONARQUBE_RUNTIME_WITHOUT_HIDDEN_FILES_SUPPORT);
    var sensor = sensor(check, binaryCheck);

    var hiddenFile = hiddenInputFile(Path.of(".env"), "{}", "secrets");
    var hiddenBinaryFile = hiddenInputFile(Path.of(".keystore"), SENSITIVE_BIDI_CHARS);
    analyse(sensor, context,
      inputFile(Path.of("a.txt"), "{}", "secrets"),
      hiddenFile,
      hiddenBinaryFile);

    var fileMeasure = TelemetryReporter.KEY_PREFIX + Analyzer.ANALYZED_FILES_MEASURE_KEY;
    var hiddenFileMeasure = TelemetryReporter.KEY_PREFIX + Analyzer.ANALYZED_HIDDEN_FILES_MEASURE_KEY;
    verify(hiddenFile, never()).isHidden();
    verify(hiddenBinaryFile, never()).isHidden();
    verify(context, never()).addTelemetryProperty(eq(hiddenFileMeasure), any());

    var expectedFilesCount = isPublicSensor() ? "2" : "3";
    verify(context).addTelemetryProperty(fileMeasure, expectedFilesCount);
  }

  @Test
  void shouldReportZeroTrackedTextFilesWhenGitStatusIsUnsuccessful() {
    var check = new ReportIssueAtLineOneCheck();
    var context = spy(sensorContext(check));
    var sensor = spy(sensor(check));
    var gitService = mock(GitService.class);
    when(gitService.retrieveUntrackedFileNames()).thenReturn(new GitService.UntrackedFileNamesResult(false, Set.of()));
    when(sensor.createGitService(any())).thenReturn(gitService);

    analyse(sensor, context,
      inputFile(Path.of("a.txt"), "{}", "secrets"),
      inputFile(Path.of("b.txt"), "{}"));

    verify(context).addTelemetryProperty(TelemetryReporter.KEY_PREFIX + TextAndSecretsSensor.ALL_TRACKED_TEXT_FILES_MEASURE_KEY, "0");
  }

  protected void assertCorrectLogsForTextAndSecretsAnalysis(int numberOfAnalyzedFiles, String... additionalLogs) {
    assertCorrectLogsForTextAndSecretsAnalysis(numberOfAnalyzedFiles, true, additionalLogs);
  }

  protected void assertCorrectLogsForTextAndSecretsAnalysis(int numberOfAnalyzedFiles, boolean withAutoTestDetection, String... additionalLogs) {
    List<String> logs = logTester.logs();

    assertThat(logs).contains(
      EXPECTED_PROCESSOR_LOG_LINE,
      DEFAULT_THREAD_USAGE_LOG_LINE);

    int numberOfMemoryLogs = 0;

    if (withAutoTestDetection) {
      assertThat(logs).contains(EXPECTED_SONAR_TEST_NOT_SET_LOG_LINE);
    }
    assertThat(logs).containsAll(Arrays.asList(additionalLogs));

    int lineAutoTest = withAutoTestDetection ? 1 : 0;
    if (numberOfAnalyzedFiles == 0) {
      assertThat(logs).hasSizeGreaterThanOrEqualTo(additionalLogs.length + numberOfMemoryLogs + lineAutoTest + 2);
    } else if (numberOfAnalyzedFiles == 1) {
      assertThat(logs).hasSizeGreaterThanOrEqualTo(additionalLogs.length + numberOfMemoryLogs + lineAutoTest + 6);
      assertThat(logs).contains(
        "1 source file to be analyzed for the text and secrets analysis",
        "1/1 source file has been analyzed for the text and secrets analysis");
    } else {
      assertThat(logs).hasSizeGreaterThanOrEqualTo(additionalLogs.length + numberOfMemoryLogs + lineAutoTest + 6);
      assertThat(logs).contains(
        numberOfAnalyzedFiles + " source files to be analyzed for the text and secrets analysis",
        numberOfAnalyzedFiles + "/" + numberOfAnalyzedFiles + " source files have been analyzed for the text and secrets analysis");
    }
  }

  private void analyseDirectory(Sensor sensor, SensorContextTester context, Path directory) throws IOException {
    try (Stream<Path> list = Files.list(directory)) {
      list.sorted().forEach(file -> context.fileSystem().add(inputFileFromPath(file)));
    }
    sensor.execute(context);
  }

  protected void analyse(Sensor sensor, SensorContextTester context, InputFile... inputFiles) {
    for (InputFile inputFile : inputFiles) {
      context.fileSystem().add(inputFile);
    }
    sensor.execute(context);
  }

  private boolean isPublicSensor() {
    return this.getClass().getName().startsWith("org");
  }

  private InputFile hiddenInputFile(Path path, @Nullable String content, @Nullable String language) {
    var inputFile = spy(inputFile(path, content, language));
    when(inputFile.isHidden()).thenReturn(true);
    return inputFile;
  }

  private InputFile hiddenInputFile(Path path, @Nullable String content) {
    return hiddenInputFile(path, content, null);
  }
}
