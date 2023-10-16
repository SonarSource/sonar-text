/*
 * SonarQube Text Plugin
 * Copyright (C) 2021-2023 SonarSource SA
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
import java.util.Set;
import java.util.stream.Stream;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mockito;
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
import static org.sonar.plugins.common.TestUtils.SONARQUBE_RUNTIME;
import static org.sonar.plugins.common.TestUtils.activeRules;
import static org.sonar.plugins.common.TestUtils.asString;
import static org.sonar.plugins.common.TestUtils.defaultSensorContext;
import static org.sonar.plugins.common.TestUtils.inputFile;
import static org.sonar.plugins.common.TestUtils.sensorContext;
import static org.sonar.plugins.common.TextAndSecretsSensor.TEXT_INCLUSIONS_DEFAULT_VALUE;

class TextAndSecretsSensorTest {

  private static final String SENSITIVE_BIDI_CHARS = "\u0002\u0004";

  @RegisterExtension
  LogTesterJUnit5 logTester = new LogTesterJUnit5();

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
    assertThat(logTester.logs()).isEmpty();
  }

  @Test
  void shouldNotStartAnalysisWhenNoFileToAnalyze() {
    SensorContextTester context = defaultSensorContext();
    sensor(context).execute(context);

    assertThat(context.allIssues()).isEmpty();
    assertThat(logTester.logs()).isEmpty();
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
    assertThat(logTester.logs()).containsExactly(
      "1 source file to be analyzed",
      "1/1 source file has been analyzed");
  }

  @Rule(key = "IssueAtLineOne")
  class ReportIssueAtLineOneCheck extends TextCheck {
    public void analyze(InputFileContext ctx) {
      ctx.reportIssue(getRuleKey(), 1, "testIssue");
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
    assertThat(logTester.logs()).containsExactly(
      "1 source file to be analyzed",
      "1/1 source file has been analyzed");
  }

  @Test
  void shouldStopOnCancellation() {
    Check check = new ReportIssueAtLineOneCheck();
    SensorContextTester context = sensorContext(check);
    context.setCancelled(true);
    analyse(sensor(check), context, inputFile("{}"));
    assertThat(context.allIssues()).isEmpty();
    assertThat(logTester.logs()).containsExactly(
      "1 source file to be analyzed");
  }

  @Test
  void issueShouldNotBeRaisedTwiceOnSameLine() {
    @Rule(key = "IssueAtLineOne")
    class ReportDuplicatedIssuesCheck extends TextCheck {
      public void analyze(InputFileContext ctx) {
        ctx.reportIssue(getRuleKey(), 1, "testIssue");
        ctx.reportIssue(getRuleKey(), 1, "testIssue");
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

    verify(check).initialize(any(), any(), any());
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
    assertThat(logTester.logs()).isEmpty();
  }

  @Test
  void shouldAnalyzeLanguageAssignedFilesInSonarQubeContext() {
    Check check = new ReportIssueAtLineOneCheck();
    SensorContextTester context = sensorContext(check);
    context.setRuntime(TestUtils.SONARQUBE_RUNTIME);
    analyse(sensor(check), context, inputFile(Path.of("Foo.java"), SENSITIVE_BIDI_CHARS, "java"));
    assertThat(logTester.logs()).containsExactly(
      "1 source file to be analyzed",
      "1/1 source file has been analyzed");
  }

  @Test
  void shouldNotAnalyzeNonLanguageAssignedFilesInSonarQubeContextWhenPropertyIsSet() {
    Check check = new ReportIssueAtLineOneCheck();
    SensorContextTester context = sensorContext(check);
    context.setRuntime(TestUtils.SONARQUBE_RUNTIME);
    context.setSettings(new MapSettings().setProperty("sonar.text.analyzeAllFiles", true));
    analyse(sensor(check), context, inputFile(Path.of("Foo.java"), SENSITIVE_BIDI_CHARS, null));
    assertThat(logTester.logs()).contains("1 source file to be analyzed");
  }

  @Test
  void shouldNotExecuteChecksOnBinaryFileNames() {
    Check check = new BoomCheck();
    SensorContextTester context = sensorContext(check);
    analyse(sensor(check), context, inputFile(Path.of("Foo.class"), "abc", null));

    // does not even contain "1/1 source file has been analyzed"
    assertThat(logTester.logs()).isEmpty();
  }

  @Test
  void shouldExecuteChecksOnIncludedTextFileNames() {
    Check check = new ReportIssueAtLineOneCheck();
    SensorContextTester context = sensorContext(check);
    context.setSettings(new MapSettings().setProperty(TextAndSecretsSensor.TEXT_INCLUSIONS_KEY, "*.txt"));
    context.setRuntime(TestUtils.SONARQUBE_RUNTIME);
    analyse(sensor(check), context, inputFile(Path.of("Foo.txt"), "abc", null));
    assertThat(logTester.logs()).contains("1 source file to be analyzed");
  }

  @Test
  void shouldNotExecuteChecksOnNonIncludedTextFileNames() {
    Check check = new ReportIssueAtLineOneCheck();
    SensorContextTester context = sensorContext(check);
    context.setSettings(new MapSettings().setProperty(TextAndSecretsSensor.TEXT_INCLUSIONS_KEY, "*.csv"));
    context.setRuntime(TestUtils.SONARQUBE_RUNTIME);
    analyse(sensor(check), context, inputFile(Path.of("Foo.txt"), "abc", null));
    assertThat(logTester.logs()).isEmpty();
  }

  @Test
  void shouldExecuteChecksOnMultipleIncludedTextFileNames() {
    Check check = new ReportIssueAtLineOneCheck();
    SensorContextTester context = sensorContext(check);
    context.setSettings(new MapSettings().setProperty(TextAndSecretsSensor.TEXT_INCLUSIONS_KEY, "*.txt,*.csv"));
    context.setRuntime(TestUtils.SONARQUBE_RUNTIME);
    analyse(sensor(check), context,
      inputFile(Path.of("Foo.txt"), SENSITIVE_BIDI_CHARS, null),
      inputFile(Path.of("Foo.csv"), SENSITIVE_BIDI_CHARS, null),
      inputFile(Path.of("Foo.nope"), "abc", null));
    assertThat(logTester.logs()).containsExactlyInAnyOrder(
      "The file 'Foo.txt' contains binary data and will not be analyzed.",
      "Please check this file and/or remove the extension from the 'sonar.text.inclusions' property.",
      "2 source files to be analyzed",
      "The file 'Foo.csv' contains binary data and will not be analyzed.",
      "Please check this file and/or remove the extension from the 'sonar.text.inclusions' property.",
      "2/2 source files have been analyzed");
  }

  @Test
  void shouldNotExecuteChecksOnMultipleIncludedTextFileNamesWithoutAstrix() {
    Check check = new ReportIssueAtLineOneCheck();
    SensorContextTester context = sensorContext(check);
    context.setSettings(new MapSettings().setProperty(TextAndSecretsSensor.TEXT_INCLUSIONS_KEY, ".txt,.csv"));
    context.setRuntime(TestUtils.SONARQUBE_RUNTIME);
    analyse(sensor(check), context,
      inputFile(Path.of("Foo.txt"), "abc", null),
      inputFile(Path.of("Foo.csv"), "abc", null),
      inputFile(Path.of("Foo.nope"), "abc", null));
    assertThat(logTester.logs()).isEmpty();
  }

  @Test
  void shouldExecuteChecksOnDotEnvFile() {
    Check check = new ReportIssueAtLineOneCheck();
    SensorContextTester context = sensorContext(check);
    context.setSettings(new MapSettings().setProperty(TextAndSecretsSensor.TEXT_INCLUSIONS_KEY, ".env"));
    context.setRuntime(TestUtils.SONARQUBE_RUNTIME);
    analyse(sensor(check), context,
      inputFile(Path.of(".env"), SENSITIVE_BIDI_CHARS, null),
      inputFile(Path.of("Foo.env"), SENSITIVE_BIDI_CHARS, null),
      inputFile(Path.of(".environment"), SENSITIVE_BIDI_CHARS, null),
      inputFile(Path.of("Foo.environment"), SENSITIVE_BIDI_CHARS, null));
    assertThat(logTester.logs()).containsExactlyInAnyOrder(
      "1 source file to be analyzed",
      "The file '.env' contains binary data and will not be analyzed.",
      "Please check this file and/or remove the extension from the 'sonar.text.inclusions' property.",
      "1/1 source file has been analyzed");
  }

  @Test
  void shouldExecuteChecksOnDotAwsConfig() {
    Check check = new ReportIssueAtLineOneCheck();
    SensorContextTester context = sensorContext(check);
    context.setSettings(new MapSettings().setProperty(TextAndSecretsSensor.TEXT_INCLUSIONS_KEY, ".aws/config"));
    context.setRuntime(TestUtils.SONARQUBE_RUNTIME);
    analyse(sensor(check), context,
      inputFile(Path.of(".aws", "config"), SENSITIVE_BIDI_CHARS, null),
      inputFile(Path.of(".aws-config"), SENSITIVE_BIDI_CHARS, null),
      inputFile(Path.of(".aws/configuration"), SENSITIVE_BIDI_CHARS, null),
      inputFile(Path.of("config"), SENSITIVE_BIDI_CHARS, null));
    assertThat(logTester.logs()).containsExactlyInAnyOrder(
      "The file '.aws/config' contains binary data and will not be analyzed.",
      "Please check this file and/or remove the extension from the 'sonar.text.inclusions' property.",
      "1 source file to be analyzed",
      "1/1 source file has been analyzed");
  }

  @Test
  void shouldExecuteChecksOnDefaults() {
    Check check = new ReportIssueAtLineOneCheck();
    SensorContextTester context = sensorContext(check);
    // INCLUDED_FILE_SUFFIXES_KEY is set to default value
    context.setSettings(new MapSettings().setProperty(TextAndSecretsSensor.TEXT_INCLUSIONS_KEY, TEXT_INCLUSIONS_DEFAULT_VALUE));
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
      "11 source files to be analyzed",
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
      "The file 'gradle.properties' contains binary data and will not be analyzed.",
      "11/11 source files have been analyzed");
  }

  @Test
  void shouldExecuteChecksOnIncludedTextFileNamesWithBinaryData() {
    Check check = new ReportIssueAtLineOneCheck();
    SensorContextTester context = sensorContext(check);
    context.setSettings(new MapSettings().setProperty(TextAndSecretsSensor.TEXT_INCLUSIONS_KEY, "*.txt"));
    context.setRuntime(TestUtils.SONARQUBE_RUNTIME);
    analyse(sensor(check), context, inputFile(Path.of("Foo.txt"), SENSITIVE_BIDI_CHARS, null));
    assertThat(logTester.logs()).containsExactlyInAnyOrder(
      "The file 'Foo.txt' contains binary data and will not be analyzed.",
      "1 source file to be analyzed",
      "Please check this file and/or remove the extension from the 'sonar.text.inclusions' property.",
      "1/1 source file has been analyzed");
  }

  @Test
  void shouldNotExcludeBinaryFileContentIfLanguageIsNotNull() {
    Check check = new ReportIssueAtLineOneCheck();
    SensorContextTester context = defaultSensorContext();
    context.setRuntime(TestUtils.SONARQUBE_RUNTIME);
    analyse(sensor(check), context, inputFile(Path.of("Foo.java"), SENSITIVE_BIDI_CHARS, "java"));

    assertThat(asString(context.allIssues())).containsExactly(
      "text:IssueAtLineOne [1:0-1:2] testIssue");
    assertThat(logTester.logs()).containsExactly(
      "1 source file to be analyzed",
      "1/1 source file has been analyzed");
  }

  @Test
  void shouldNotExcludeBinaryFileContentIfLanguageIsNullAndExtensionIncludedWithSonarqube() {
    Check check = new ReportIssueAtLineOneCheck();
    SensorContextTester context = defaultSensorContext();
    context.setRuntime(TestUtils.SONARQUBE_RUNTIME);
    context.setSettings(new MapSettings().setProperty(TextAndSecretsSensor.TEXT_INCLUSIONS_KEY, "*.txt"));
    analyse(sensor(check), context,
      inputFile(Path.of("Foo.txt"), SENSITIVE_BIDI_CHARS, null),
      inputFile(Path.of("FileWithoutExtension"), SENSITIVE_BIDI_CHARS, null));

    assertThat(logTester.logs()).containsExactlyInAnyOrder(
      "The file 'Foo.txt' contains binary data and will not be analyzed.",
      "1 source file to be analyzed",
      "Please check this file and/or remove the extension from the 'sonar.text.inclusions' property.",
      "1/1 source file has been analyzed");
  }

  @Test
  void shouldExcludeBinaryFileContentIfLanguageIsNullWithSonarlint() {
    Check check = new ReportIssueAtLineOneCheck();
    SensorContextTester context = defaultSensorContext();
    analyse(sensor(check), context,
      inputFile(Path.of("Foo.txt"), SENSITIVE_BIDI_CHARS, null),
      inputFile(Path.of("FileWithoutExtension"), SENSITIVE_BIDI_CHARS, null));

    assertThat(asString(context.allIssues())).isEmpty();
    assertThat(logTester.logs()).containsExactlyInAnyOrder(
      "2 source files to be analyzed",
      "'txt' was added to the binary file filter because the file 'Foo.txt' is a binary file.",
      "To remove the previous warning you can add the '.txt' extension to the 'sonar.text.excluded.file.suffixes' property.",
      "2/2 source files have been analyzed");
  }

  @Test
  void shouldExcludeBinaryFileExtensionDynamically() throws IOException {
    Check check = new BoomCheck();
    SensorContextTester context = sensorContext(check);
    analyseDirectory(sensor(check), context, Path.of("src", "test", "resources", "binary-files"));
    assertThat(logTester.logs()).containsExactlyInAnyOrder(
      // 4 and not 5 because Foo.class is excluded
      "4 source files to be analyzed",
      // Because of this warning about 'Foo.unknown1' we will not have any error about 'Bar.unknown1'
      "'unknown1' was added to the binary file filter because the file 'src/test/resources/binary-files/Foo.unknown1' is a binary file.",
      // help is displayed only once for '.unknown1'
      "To remove the previous warning you can add the '.unknown1' extension to the 'sonar.text.excluded.file.suffixes' property.",
      "'unknown2' was added to the binary file filter because the file 'src/test/resources/binary-files/Foo.unknown2' is a binary file.",
      "4/4 source files have been analyzed");
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
    context.setRuntime(SONARQUBE_RUNTIME);
    var sensor = sensor(check);
    var spy = Mockito.spy(sensor);
    var gitSupplier = mock(GitSupplier.class);
    Path fooJavaPath = Path.of("src", "foo.java");
    String relativePathFooJava = "src" + fooJavaPath.getFileSystem().getSeparator() + "foo.java";
    var gitMock = setupGitMock(Set.of("a.txt", relativePathFooJava));
    when(gitSupplier.getGit()).thenReturn(gitMock);
    when(spy.getGitSupplier()).thenReturn(gitSupplier);

    analyse(spy, context,
      inputFile(Path.of("a.txt"), "{}", "secrets"),
      inputFile(Path.of("b.txt"), "{}", "secrets"),
      inputFile(fooJavaPath));

    Collection<Issue> issues = context.allIssues();
    assertThat(issues)
      .hasSize(1)
      .map(it -> ((InputFile) it.primaryLocation().inputComponent()).filename())
      .containsExactly("b.txt");
    assertThat(logTester.logs().get(0)).endsWith("1 source file to be analyzed");
  }

  @Test
  void shouldNotCallGitFilePredicateInSonarlintContext() throws IOException, GitAPIException {
    Check check = new ReportIssueAtLineOneCheck();
    SensorContextTester context = sensorContext(check);
    var sensor = sensor(check);
    var spy = Mockito.spy(sensor);
    var gitSupplier = mock(GitSupplier.class);
    var gitMock = setupGitMock(Set.of("a.txt"));
    when(gitSupplier.getGit()).thenReturn(gitMock);
    when(spy.getGitSupplier()).thenReturn(gitSupplier);

    analyse(spy, context,
      inputFile(Path.of("a.txt"), "{}"),
      inputFile(Path.of("b.txt"), "{}"));

    Collection<Issue> issues = context.allIssues();
    assertThat(issues).hasSize(2);
    assertThat(logTester.logs().get(0)).endsWith("2 source files to be analyzed");
    verify(gitSupplier, times(0)).getGit();
    verify(spy, times(0)).getGitSupplier();
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
