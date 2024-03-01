/*
 * SonarQube Text Plugin
 * Copyright (C) 2021-2024 SonarSource SA
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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.slf4j.event.Level;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.error.AnalysisError;
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.batch.sensor.issue.Issue;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonar.check.Rule;
import org.sonar.plugins.secrets.api.SpecificationBasedCheck;
import org.sonar.plugins.secrets.api.task.RegexMatchingManager;
import org.sonar.plugins.text.api.TextCheck;
import org.sonar.plugins.text.checks.BIDICharacterCheck;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.plugins.common.GitTrackedFilePredicateTest.setupGitMock;
import static org.sonar.plugins.common.TestUtils.SONARCLOUD_RUNTIME;
import static org.sonar.plugins.common.TestUtils.SONARQUBE_RUNTIME;
import static org.sonar.plugins.common.TestUtils.activeRules;
import static org.sonar.plugins.common.TestUtils.asString;
import static org.sonar.plugins.common.TestUtils.defaultSensorContext;
import static org.sonar.plugins.common.TestUtils.inputFile;
import static org.sonar.plugins.common.TestUtils.sensorContext;
import static org.sonar.plugins.common.TextAndSecretsSensor.TEXT_INCLUSIONS_DEFAULT_VALUE;

class TextAndSecretsSensorTest {

  private static final String SENSITIVE_BIDI_CHARS = "\u0002\u0004";
  private static final int AVAILABLE_PROCESSORS = Runtime.getRuntime().availableProcessors();
  private static final String EXPECTED_PROCESSOR_LOG_LINE = "Available processors: " + AVAILABLE_PROCESSORS;
  private static final String DEFAULT_THREAD_USAGE_LOG_LINE = "Using " + AVAILABLE_PROCESSORS + " threads for analysis.";

  @RegisterExtension
  LogTesterJUnit5 logTester = new LogTesterJUnit5();

  @AfterEach
  public void cleanUp() {
    int defaultTimeout = 10_000;
    // due to running other tests, this property can be changed. That's why we need to set the default after each test.
    RegexMatchingManager.setTimeoutMs(defaultTimeout);
    RegexMatchingManager.setUninterruptibleTimeoutMs(defaultTimeout);
  }

  @Test
  void shouldDescribeWithoutErrors() {
    DefaultSensorDescriptor descriptor = new DefaultSensorDescriptor();
    sensor(defaultSensorContext()).describe(descriptor);

    assertThat(descriptor.name()).isEqualTo("TextAndSecretsSensor");
    assertThat(descriptor.languages()).isEmpty();
    assertThat(descriptor.isProcessesFilesIndependently()).isTrue();
    assertThat(descriptor.ruleRepositories()).containsExactlyInAnyOrder("text", "secrets");
    assertThat(logTester.logs()).isEmpty();
  }

  @Test
  void shouldNotStartAnalysisWhenNoRuleIsActive() {
    String[] emptyActiveRuleList = {};
    SensorContextTester context = sensorContext(emptyActiveRuleList);
    context.fileSystem().add(inputFile(Path.of("src", "test", "resources", "checks", "GoogleCloudAccountKeyCheck",
      "GoogleCloudAccountPositive.json")));
    sensor(context).execute(context);

    assertThat(context.allIssues()).isEmpty();
    assertCorrectLogs(logTester.logs(), 0);
  }

  @Test
  void shouldNotStartAnalysisWhenNoFileToAnalyze() {
    SensorContextTester context = defaultSensorContext();
    sensor(context).execute(context);

    assertThat(context.allIssues()).isEmpty();
    assertCorrectLogs(logTester.logs(), 0);
  }

  @Test
  void shouldNotRaiseAnIssueOrErrorWhenTheInputFileDoesNotExist() {
    SensorContextTester context = defaultSensorContext();
    context.fileSystem().add(inputFile(Path.of("invalid-path.txt")));

    sensor(context).execute(context);

    assertThat(context.allIssues()).isEmpty();
    assertThat(logTester.logs()).anyMatch(log -> log.startsWith("Unable to analyze file") && log.contains("invalid-path.txt"));
  }

  @Test
  void emptyFileShouldRaiseNoIssue() {
    SensorContextTester context = defaultSensorContext();
    analyse(sensor(context), context, inputFile(""));
    assertThat(context.allIssues()).isEmpty();
    assertCorrectLogs(logTester.logs(), 1);
  }

  @Rule(key = "IssueAtLineOne")
  class ReportIssueAtLineOneCheck extends TextCheck {
    public void analyze(InputFileContext ctx) {
      ctx.reportTextIssue(getRuleKey(), 1, "testIssue");
    }
  }

  @Test
  void validCheckOnValidFileShouldRaiseIssue() {
    Check check = new ReportIssueAtLineOneCheck();
    InputFile inputFile = inputFile("foo");
    SensorContextTester context = defaultSensorContext();
    analyse(sensor(check), context, inputFile);

    assertThat(asString(context.allIssues())).containsExactly(
      "text:IssueAtLineOne [1:0-1:3] testIssue");
    assertCorrectLogs(logTester.logs(), 1);
  }

  @Test
  void shouldStopOnCancellation() {
    Check check = new ReportIssueAtLineOneCheck();
    SensorContextTester context = sensorContext(check);
    context.setCancelled(true);
    analyse(sensor(check), context, inputFile("{}"));
    assertThat(context.allIssues()).isEmpty();
    assertThat(logTester.logs()).containsExactly(
      EXPECTED_PROCESSOR_LOG_LINE,
      DEFAULT_THREAD_USAGE_LOG_LINE,
      "1 source file to be analyzed");
  }

  @Test
  void analysisErrorShouldBeRaisedOnFailureInCheck() {
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
  void specificationBasedCheckShouldBeInitializedAndReportErrorLog() {
    @Rule(key = "SecretKey")
    class SpecificationLoadingCheck extends SpecificationBasedCheck {
    }
    SpecificationLoadingCheck check = spy(new SpecificationLoadingCheck());
    SensorContextTester context = sensorContext(check);
    InputFile inputFile = inputFile("foo");
    analyse(sensor(check), context, inputFile);

    verify(check).initialize(any(), any());
    assertThat(logTester.logs()).contains("Found no rule specification for rule with key: SecretKey");
  }

  @Rule(key = "Boom")
  class BoomCheck extends TextCheck {
    public void analyze(InputFileContext ctx) {
      throw new IllegalStateException("Should not occurs because there are only binary files");
    }
  }

  @Test
  void shouldNotAnalyzeNonLanguageAssignedFilesInSonarQubeContext() {
    Check check = new ReportIssueAtLineOneCheck();
    SensorContextTester context = sensorContext(check);
    context.setRuntime(TestUtils.SONARQUBE_RUNTIME);
    analyse(sensor(check), context, inputFile(Path.of("Foo.java"), SENSITIVE_BIDI_CHARS, null));
    assertCorrectLogs(logTester.logs(), 0);
  }

  @Test
  void shouldAnalyzeLanguageAssignedFilesInSonarQubeContext() {
    Check check = new ReportIssueAtLineOneCheck();
    SensorContextTester context = sensorContext(check);
    context.setRuntime(TestUtils.SONARQUBE_RUNTIME);
    analyse(sensor(check), context, inputFile(Path.of("Foo.java"), SENSITIVE_BIDI_CHARS, "java"));
    assertCorrectLogs(logTester.logs(), 1);
  }

  @Test
  void shouldNotAnalyzeNonLanguageAssignedFilesInSonarQubeContextWhenPropertyIsSet() {
    Check check = new ReportIssueAtLineOneCheck();
    SensorContextTester context = sensorContext(check);
    context.setRuntime(TestUtils.SONARQUBE_RUNTIME);
    context.setSettings(new MapSettings().setProperty("sonar.text.analyzeAllFiles", true));
    analyse(sensor(check), context, inputFile(Path.of("Foo.java"), SENSITIVE_BIDI_CHARS, null));
    assertCorrectLogs(logTester.logs(), 0,
      "'java' was added to the binary file filter because the file 'Foo.java' is a binary file.",
      "To remove the previous warning you can add the '.java' extension to the 'sonar.text.excluded.file.suffixes' property.");
  }

  @Test
  void shouldNotExecuteChecksOnBinaryFileNames() {
    Check check = new BoomCheck();
    SensorContextTester context = sensorContext(check);
    analyse(sensor(check), context, inputFile(Path.of("Foo.class"), "abc", null));

    // does not even contain "1/1 source file has been analyzed"
    assertCorrectLogs(logTester.logs(), 0);
  }

  @Test
  void shouldExecuteChecksOnIncludedTextFileNames() {
    Check check = new ReportIssueAtLineOneCheck();
    SensorContextTester context = sensorContext(check);
    MapSettings mapSettings = new MapSettings();
    mapSettings.setProperty(TextAndSecretsSensor.TEXT_INCLUSIONS_KEY, "*.txt");
    mapSettings.setProperty(TextAndSecretsSensor.INCLUSIONS_ACTIVATION_KEY, "true");
    context.setSettings(mapSettings);
    context.setRuntime(TestUtils.SONARQUBE_RUNTIME);
    analyse(sensor(check), context, inputFile(Path.of("Foo.txt"), "abc", null));
    assertCorrectLogs(logTester.logs(), 1);
  }

  @Test
  void shouldNotExecuteChecksOnNonIncludedTextFileNames() {
    Check check = new ReportIssueAtLineOneCheck();
    SensorContextTester context = sensorContext(check);
    MapSettings mapSettings = new MapSettings();
    mapSettings.setProperty(TextAndSecretsSensor.TEXT_INCLUSIONS_KEY, "*.csv");
    mapSettings.setProperty(TextAndSecretsSensor.INCLUSIONS_ACTIVATION_KEY, "true");
    context.setSettings(mapSettings);
    context.setRuntime(TestUtils.SONARQUBE_RUNTIME);
    analyse(sensor(check), context, inputFile(Path.of("Foo.txt"), "abc", null));
    assertCorrectLogs(logTester.logs(), 0);
  }

  @Test
  void shouldExecuteChecksOnMultipleIncludedTextFileNames() {
    Check check = new ReportIssueAtLineOneCheck();
    SensorContextTester context = sensorContext(check);
    MapSettings mapSettings = new MapSettings();
    mapSettings.setProperty(TextAndSecretsSensor.TEXT_INCLUSIONS_KEY, "*.txt,*.csv");
    mapSettings.setProperty(TextAndSecretsSensor.INCLUSIONS_ACTIVATION_KEY, "true");
    context.setSettings(mapSettings);
    context.setRuntime(TestUtils.SONARQUBE_RUNTIME);
    analyse(sensor(check), context,
      inputFile(Path.of("Foo.txt"), SENSITIVE_BIDI_CHARS, null),
      inputFile(Path.of("Foo.csv"), SENSITIVE_BIDI_CHARS, null),
      inputFile(Path.of("Foo.nope"), "abc", null));
    assertCorrectLogs(logTester.logs(), 0,
      "The file 'Foo.txt' contains binary data and will not be analyzed.",
      "Please check this file and/or remove the extension from the 'sonar.text.inclusions' property.",
      "The file 'Foo.csv' contains binary data and will not be analyzed.",
      "Please check this file and/or remove the extension from the 'sonar.text.inclusions' property.");
  }

  @Test
  void shouldNotExecuteChecksOnMultipleIncludedTextFileNamesWithoutAstrix() {
    Check check = new ReportIssueAtLineOneCheck();
    SensorContextTester context = sensorContext(check);
    MapSettings mapSettings = new MapSettings();
    mapSettings.setProperty(TextAndSecretsSensor.TEXT_INCLUSIONS_KEY, ".txt,.csv");
    mapSettings.setProperty(TextAndSecretsSensor.INCLUSIONS_ACTIVATION_KEY, "true");
    context.setSettings(mapSettings);
    context.setRuntime(TestUtils.SONARQUBE_RUNTIME);
    analyse(sensor(check), context,
      inputFile(Path.of("Foo.txt"), "abc", null),
      inputFile(Path.of("Foo.csv"), "abc", null),
      inputFile(Path.of("Foo.nope"), "abc", null));
    assertCorrectLogs(logTester.logs(), 0);
  }

  @Test
  void shouldExecuteChecksOnDotEnvFile() {
    Check check = new ReportIssueAtLineOneCheck();
    SensorContextTester context = sensorContext(check);
    MapSettings mapSettings = new MapSettings();
    mapSettings.setProperty(TextAndSecretsSensor.TEXT_INCLUSIONS_KEY, ".env");
    mapSettings.setProperty(TextAndSecretsSensor.INCLUSIONS_ACTIVATION_KEY, "true");
    context.setSettings(mapSettings);
    context.setRuntime(TestUtils.SONARQUBE_RUNTIME);
    analyse(sensor(check), context,
      inputFile(Path.of(".env"), SENSITIVE_BIDI_CHARS, null),
      inputFile(Path.of("Foo.env"), SENSITIVE_BIDI_CHARS, null),
      inputFile(Path.of(".environment"), SENSITIVE_BIDI_CHARS, null),
      inputFile(Path.of("Foo.environment"), SENSITIVE_BIDI_CHARS, null));
    assertCorrectLogs(logTester.logs(), 0,
      "The file '.env' contains binary data and will not be analyzed.",
      "Please check this file and/or remove the extension from the 'sonar.text.inclusions' property.");
  }

  @Test
  void shouldExecuteChecksOnDotAwsConfig() {
    Check check = new ReportIssueAtLineOneCheck();
    SensorContextTester context = sensorContext(check);
    MapSettings mapSettings = new MapSettings();
    mapSettings.setProperty(TextAndSecretsSensor.TEXT_INCLUSIONS_KEY, ".aws/config");
    mapSettings.setProperty(TextAndSecretsSensor.INCLUSIONS_ACTIVATION_KEY, "true");
    context.setSettings(mapSettings);
    context.setRuntime(TestUtils.SONARQUBE_RUNTIME);
    analyse(sensor(check), context,
      inputFile(Path.of(".aws", "config"), SENSITIVE_BIDI_CHARS, null),
      inputFile(Path.of(".aws-config"), SENSITIVE_BIDI_CHARS, null),
      inputFile(Path.of(".aws/configuration"), SENSITIVE_BIDI_CHARS, null),
      inputFile(Path.of("config"), SENSITIVE_BIDI_CHARS, null));
    assertCorrectLogs(logTester.logs(), 0,
      "The file '.aws/config' contains binary data and will not be analyzed.",
      "Please check this file and/or remove the extension from the 'sonar.text.inclusions' property.");
  }

  @Test
  void shouldExecuteChecksOnDefaults() {
    Check check = new ReportIssueAtLineOneCheck();
    SensorContextTester context = sensorContext(check);
    MapSettings mapSettings = new MapSettings();
    // INCLUDED_FILE_SUFFIXES_KEY is set to default value
    mapSettings.setProperty(TextAndSecretsSensor.TEXT_INCLUSIONS_KEY, TEXT_INCLUSIONS_DEFAULT_VALUE);
    mapSettings.setProperty(TextAndSecretsSensor.INCLUSIONS_ACTIVATION_KEY, "true");
    context.setSettings(mapSettings);
    context.setRuntime(TestUtils.SONARQUBE_RUNTIME);
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
      "The file 'config/some.conf' contains binary data and will not be analyzed.",
      "The file '.aws/config' contains binary data and will not be analyzed.",
      "The file 'script/start.sh' contains binary data and will not be analyzed.",
      "The file 'my.pem' contains binary data and will not be analyzed.",
      "The file '.env' contains binary data and will not be analyzed.",
      "The file 'win.ps1' contains binary data and will not be analyzed.",
      "The file 'run.bash' contains binary data and will not be analyzed.",
      "The file 'ccc.config' contains binary data and will not be analyzed.",
      "The file 'a.zsh' contains binary data and will not be analyzed.",
      "The file 'b.ksh' contains binary data and will not be analyzed.",
      "The file 'gradle.properties' contains binary data and will not be analyzed.");
  }

  @Test
  void shouldExecuteChecksOnIncludedTextFileNamesWithBinaryData() {
    Check check = new ReportIssueAtLineOneCheck();
    SensorContextTester context = sensorContext(check);
    MapSettings mapSettings = new MapSettings();
    // INCLUDED_FILE_SUFFIXES_KEY is set to default value
    mapSettings.setProperty(TextAndSecretsSensor.TEXT_INCLUSIONS_KEY, "*.txt");
    mapSettings.setProperty(TextAndSecretsSensor.INCLUSIONS_ACTIVATION_KEY, "true");
    context.setSettings(mapSettings);
    context.setRuntime(TestUtils.SONARQUBE_RUNTIME);
    analyse(sensor(check), context, inputFile(Path.of("Foo.txt"), SENSITIVE_BIDI_CHARS, null));
    assertCorrectLogs(logTester.logs(), 0,
      "The file 'Foo.txt' contains binary data and will not be analyzed.",
      "Please check this file and/or remove the extension from the 'sonar.text.inclusions' property.");
  }

  @Test
  void shouldNotExcludeBinaryFileContentIfLanguageIsNotNull() {
    Check check = new ReportIssueAtLineOneCheck();
    SensorContextTester context = defaultSensorContext();
    context.setRuntime(TestUtils.SONARQUBE_RUNTIME);
    analyse(sensor(check), context, inputFile(Path.of("Foo.java"), SENSITIVE_BIDI_CHARS, "java"));

    assertThat(asString(context.allIssues())).containsExactly(
      "text:IssueAtLineOne [1:0-1:2] testIssue");
    assertCorrectLogs(logTester.logs(), 1);
  }

  @Test
  void shouldNotExcludeBinaryFileContentIfLanguageIsNullAndExtensionIncludedWithSonarqube() {
    Check check = new ReportIssueAtLineOneCheck();
    SensorContextTester context = defaultSensorContext();
    context.setRuntime(TestUtils.SONARQUBE_RUNTIME);
    MapSettings mapSettings = new MapSettings();
    mapSettings.setProperty(TextAndSecretsSensor.TEXT_INCLUSIONS_KEY, "*.txt");
    mapSettings.setProperty(TextAndSecretsSensor.INCLUSIONS_ACTIVATION_KEY, "true");
    context.setSettings(mapSettings);
    analyse(sensor(check), context,
      inputFile(Path.of("Foo.txt"), SENSITIVE_BIDI_CHARS, null),
      inputFile(Path.of("FileWithoutExtension"), SENSITIVE_BIDI_CHARS, null));

    assertCorrectLogs(logTester.logs(), 0,
      "The file 'Foo.txt' contains binary data and will not be analyzed.",
      "Please check this file and/or remove the extension from the 'sonar.text.inclusions' property.");
  }

  @Test
  void shouldExcludeBinaryFileContentIfLanguageIsNullWithSonarlint() {
    Check check = new ReportIssueAtLineOneCheck();
    SensorContextTester context = defaultSensorContext();
    analyse(sensor(check), context,
      inputFile(Path.of("Foo.txt"), SENSITIVE_BIDI_CHARS, null),
      inputFile(Path.of("FileWithoutExtension"), SENSITIVE_BIDI_CHARS, null));

    assertThat(asString(context.allIssues())).isEmpty();
    assertCorrectLogs(logTester.logs(), 0,
      "'txt' was added to the binary file filter because the file 'Foo.txt' is a binary file.",
      "To remove the previous warning you can add the '.txt' extension to the 'sonar.text.excluded.file.suffixes' property.");
  }

  @Test
  void shouldExcludeBinaryFileExtensionDynamically() throws IOException {
    Check check = new BoomCheck();
    SensorContextTester context = sensorContext(check);
    analyseDirectory(sensor(check), context, Path.of("src", "test", "resources", "binary-files"));
    assertCorrectLogs(logTester.logs(), 0,
      "'unknown1' was added to the binary file filter because the file 'src/test/resources/binary-files/Foo.unknown1' is a binary file.",
      // Because of this warning about 'Foo.unknown1' we will not have any error about 'Bar.unknown1'
      "'unknown2' was added to the binary file filter because the file 'src/test/resources/binary-files/Foo.unknown2' is a binary file.",
      // help is displayed only once for '.unknown1'
      "To remove the previous warning you can add the '.unknown1' extension to the 'sonar.text.excluded.file.suffixes' property.");
  }

  @Test
  void analysisErrorShouldBeRaisedOnCorruptedFile() throws IOException {
    Check check = new BIDICharacterCheck();
    SensorContextTester context = sensorContext(check);
    context.setRuntime(SONARQUBE_RUNTIME);
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
  void shouldNotAnalyzeUntrackedFiles() throws IOException, GitAPIException {
    Check check = new ReportIssueAtLineOneCheck();
    SensorContextTester context = sensorContext(check);
    MapSettings mapSettings = new MapSettings();
    mapSettings.setProperty(TextAndSecretsSensor.INCLUSIONS_ACTIVATION_KEY, "true");
    context.setSettings(mapSettings);
    context.setRuntime(SONARQUBE_RUNTIME);
    var sensor = sensor(check);
    var sensorSpy = Mockito.spy(sensor);
    var gitSupplier = mock(GitSupplier.class);
    Path fooJavaPath = Path.of("src", "foo.java");
    String relativePathFooJava = "src" + fooJavaPath.getFileSystem().getSeparator() + "foo.java";
    var gitMock = setupGitMock(Set.of("a.txt", relativePathFooJava));
    when(gitSupplier.getGit()).thenReturn(gitMock);
    when(sensorSpy.getGitSupplier()).thenReturn(gitSupplier);

    analyse(sensorSpy, context,
      inputFile(Path.of("a.txt"), "{}", "secrets"),
      inputFile(Path.of("b.txt"), "{}", "secrets"),
      inputFile(fooJavaPath));

    Collection<Issue> issues = context.allIssues();
    assertThat(issues)
      .hasSize(1)
      .map(it -> ((InputFile) it.primaryLocation().inputComponent()).filename())
      .containsExactly("b.txt");
    assertCorrectLogs(logTester.logs(), 1);
  }

  @Test
  void shouldNotCallGitFilePredicateInSonarlintContext() throws IOException, GitAPIException {
    Check check = new ReportIssueAtLineOneCheck();
    SensorContextTester context = sensorContext(check);
    var sensor = sensor(check);
    var sensorSpy = Mockito.spy(sensor);
    var gitSupplier = mock(GitSupplier.class);
    var gitMock = setupGitMock(Set.of("a.txt"));
    when(gitSupplier.getGit()).thenReturn(gitMock);
    when(sensorSpy.getGitSupplier()).thenReturn(gitSupplier);

    analyse(sensorSpy, context,
      inputFile(Path.of("a.txt"), "{}"),
      inputFile(Path.of("b.txt"), "{}"));

    Collection<Issue> issues = context.allIssues();
    assertThat(issues).hasSize(2);
    assertCorrectLogs(logTester.logs(), 2);
    verify(gitSupplier, times(0)).getGit();
    verify(sensorSpy, times(0)).getGitSupplier();
  }

  @Test
  void shouldNotCallGitFilePredicateOnDefault() throws IOException, GitAPIException {
    logTester.setLevel(Level.DEBUG);
    Check check = new ReportIssueAtLineOneCheck();
    SensorContextTester context = sensorContext(check);
    context.setRuntime(SONARQUBE_RUNTIME);
    var sensor = sensor(check);
    var sensorSpy = Mockito.spy(sensor);
    var gitSupplier = mock(GitSupplier.class);
    var gitMock = setupGitMock(Set.of("a.txt"));
    when(gitSupplier.getGit()).thenReturn(gitMock);
    when(sensorSpy.getGitSupplier()).thenReturn(gitSupplier);

    analyse(sensorSpy, context,
      inputFile(Path.of("a.txt"), "{}", "secrets"),
      inputFile(Path.of("b.txt"), "{}", "secrets"));

    Collection<Issue> issues = context.allIssues();
    assertThat(issues).hasSize(2);
    assertCorrectLogs(logTester.logs(), 2,
      "Analyzing only language associated files, \"sonar.text.inclusions.activate\" property is deactivated");
    verify(gitSupplier, times(0)).getGit();
    verify(sensorSpy, times(0)).getGitSupplier();
  }

  @Test
  void shouldOnlyAnalyzeFilesBelongingToALanguageNoGitRepositoryIsFoundEvenIfInclusionsKeySet() throws IOException {
    logTester.setLevel(Level.DEBUG);
    Check check = new ReportIssueAtLineOneCheck();
    SensorContextTester context = sensorContext(check);
    context.setRuntime(SONARQUBE_RUNTIME);
    MapSettings mapSettings = new MapSettings();
    mapSettings.setProperty(TextAndSecretsSensor.INCLUSIONS_ACTIVATION_KEY, "true");
    mapSettings.setProperty(TextAndSecretsSensor.TEXT_INCLUSIONS_KEY, "*.txt");
    context.setSettings(mapSettings);
    var sensor = sensor(check);
    var sensorSpy = Mockito.spy(sensor);
    var gitSupplier = mock(GitSupplier.class);
    // GitTrackedFilePredicate.isGitStatusSuccessful will return false
    when(gitSupplier.getGit()).thenThrow(RuntimeException.class);
    when(sensorSpy.getGitSupplier()).thenReturn(gitSupplier);

    analyse(sensorSpy, context,
      inputFile(Path.of("a.txt"), "{}", "secrets"),
      inputFile(Path.of("b.txt"), "{}"));

    Collection<Issue> issues = context.allIssues();
    assertThat(issues).hasSize(1);
    assertCorrectLogs(logTester.logs(), 1,
      "Unable to retrieve git status",
      "Analyzing only language associated files, make sure to run the analysis " +
        "inside a git repository to make use of inclusions specified via \"sonar.text.inclusions\"");
  }

  @Test
  void shouldUsePropertyDefinedTimeoutValues() {
    Check check = new ReportIssueAtLineOneCheck();
    SensorContextTester context = sensorContext(check);
    MapSettings mapSettings = new MapSettings();
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
    MapSettings mapSettings = new MapSettings();
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
    MapSettings mapSettings = new MapSettings();
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
    MapSettings mapSettings = new MapSettings();
    mapSettings.setProperty(TextAndSecretsSensor.THREAD_NUMBER_KEY, "1");
    context.setSettings(mapSettings);
    analyse(sensor(check), context,
      inputFile(Path.of("a.txt"), "foo", "secrets"),
      inputFile(Path.of("b.java"), "bar", "java"),
      inputFile(Path.of("c.c"), "abc", "c"));
    assertThat(logTester.logs()).containsExactlyInAnyOrder(
      "Available processors: " + Runtime.getRuntime().availableProcessors(),
      "Using 1 thread for analysis, according to the value of \"sonar.text.threads\" property.",
      "3 source files to be analyzed",
      "3/3 source files have been analyzed");
  }

  @Test
  void shouldExecuteAnalysisWithMoreThreadsThanAvailableProcessors() {
    int availableProcessors = Runtime.getRuntime().availableProcessors();
    String usedThreads = String.valueOf(availableProcessors + 1);
    Check check = new ReportIssueAtLineOneCheck();
    SensorContextTester context = sensorContext(check);
    MapSettings mapSettings = new MapSettings();
    mapSettings.setProperty(TextAndSecretsSensor.THREAD_NUMBER_KEY, usedThreads);
    context.setSettings(mapSettings);
    analyse(sensor(check), context,
      inputFile(Path.of("a.txt"), "foo", "secrets"),
      inputFile(Path.of("b.java"), "bar", "java"),
      inputFile(Path.of("c.c"), "abc", "c"));
    assertThat(logTester.logs()).containsExactlyInAnyOrder(
      "Available processors: " + availableProcessors,
      "Using " + usedThreads + " threads for analysis, according to the value of \"sonar.text.threads\" property.",
      "\"sonar.text.threads\" property was set to " + usedThreads + ", which is greater than the number of available processors: " + availableProcessors + ".\n" +
        "It is recommended to let the analyzer detect the number of threads automatically by not setting the property.\n" +
        "For more information, visit the documentation page.",
      "3 source files to be analyzed",
      "3/3 source files have been analyzed");
  }

  @Test
  void shouldExecuteAnalysisInSonarCloudRuntime() {
    int availableProcessors = Runtime.getRuntime().availableProcessors();
    Check check = new ReportIssueAtLineOneCheck();
    SensorContextTester context = sensorContext(check);
    MapSettings mapSettings = new MapSettings();
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
      "3 source files to be analyzed",
      "3/3 source files have been analyzed");
  }

  private void assertCorrectLogs(List<String> logs, int numberOfAnalyzedFiles, String... additionalLogs) {
    assertThat(logs).contains(
      EXPECTED_PROCESSOR_LOG_LINE,
      DEFAULT_THREAD_USAGE_LOG_LINE);
    assertThat(logs).containsAll(Arrays.asList(additionalLogs));

    if (numberOfAnalyzedFiles == 0) {
      assertThat(logs).hasSize(additionalLogs.length + 2);
    } else if (numberOfAnalyzedFiles == 1) {
      assertThat(logs).hasSize(additionalLogs.length + 4);
      assertThat(logs).contains(
        "1 source file to be analyzed",
        "1/1 source file has been analyzed");
    } else {
      assertThat(logs).hasSize(additionalLogs.length + 4);
      assertThat(logs).contains(
        numberOfAnalyzedFiles + " source files to be analyzed",
        numberOfAnalyzedFiles + "/" + numberOfAnalyzedFiles + " source files have been analyzed");
    }

  }

  private void analyseDirectory(Sensor sensor, SensorContextTester context, Path directory) throws IOException {
    try (Stream<Path> list = Files.list(directory)) {
      list.sorted().forEach(file -> context.fileSystem().add(inputFile(file)));
    }
    sensor.execute(context);
  }

  private void analyse(Sensor sensor, SensorContextTester context, InputFile... inputFiles) {
    for (InputFile inputFile : inputFiles) {
      context.fileSystem().add(inputFile);
    }
    sensor.execute(context);
  }

  private TextAndSecretsSensor sensor(Check check) {
    CheckFactory checkFactory = new CheckFactory(activeRules(check.getRuleKey().toString()));
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
