/*
 * SonarQube Text Plugin
 * Copyright (C) 2021-2026 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.sonar.api.SonarEdition;
import org.sonar.api.SonarQubeSide;
import org.sonar.api.SonarRuntime;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.rule.internal.ActiveRulesBuilder;
import org.sonar.api.batch.rule.internal.DefaultActiveRules;
import org.sonar.api.batch.rule.internal.NewActiveRule;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.batch.sensor.issue.Issue;
import org.sonar.api.batch.sensor.issue.IssueLocation;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.internal.SonarRuntimeImpl;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.Version;
import org.sonar.plugins.common.measures.DurationStatistics;
import org.sonar.plugins.common.warnings.AnalysisWarningsWrapper;
import org.sonar.plugins.secrets.SecretsCheckList;
import org.sonar.plugins.text.TextCheckList;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestUtils {

  public static final Version LATEST_API_VERSION = Version.create(12, 0);
  public static final SonarRuntime SONARLINT_RUNTIME = SonarRuntimeImpl.forSonarLint(LATEST_API_VERSION);
  public static final SonarRuntime SONARQUBE_RUNTIME = SonarRuntimeImpl.forSonarQube(LATEST_API_VERSION, SonarQubeSide.SERVER, SonarEdition.ENTERPRISE);
  public static final SonarRuntime SONARCLOUD_RUNTIME = SonarRuntimeImpl.forSonarQube(LATEST_API_VERSION, SonarQubeSide.SERVER, SonarEdition.SONARCLOUD);
  public static final SonarRuntime SONARQUBE_RUNTIME_WITHOUT_TELEMETRY_SUPPORT = SonarRuntimeImpl.forSonarQube(Version.create(10, 8), SonarQubeSide.SERVER,
    SonarEdition.ENTERPRISE);
  public static final SonarRuntime SONARQUBE_RUNTIME_WITHOUT_HIDDEN_FILES_SUPPORT = SonarRuntimeImpl.forSonarQube(Version.create(11, 4), SonarQubeSide.SERVER,
    SonarEdition.ENTERPRISE);

  public static List<String> analyze(Check check, String fileContent) throws IOException {
    return analyze(check, inputFile(fileContent));
  }

  public static List<String> analyze(Check check, InputFile inputFile) throws IOException {
    SensorContextTester context = sensorContext(check);
    InputFileContext inputFileContext = new InputFileContext(context, inputFile);
    check.analyze(inputFileContext);
    inputFileContext.flushIssues();
    return asString(context.allIssues());
  }

  public static List<String> asString(Collection<Issue> issues) {
    return issues.stream().map(TestUtils::asString).toList();
  }

  public static String asString(Issue issue) {
    IssueLocation location = issue.primaryLocation();
    TextRange range = location.textRange();
    if (range == null) {
      return String.format("%s [] %s", issue.ruleKey(), location.message());
    }
    return String.format("%s [%d:%d-%d:%d] %s", issue.ruleKey(), range.start().line(), range.start().lineOffset(),
      range.end().line(), range.end().lineOffset(), location.message());
  }

  public static InputFile inputFile(String fileContent) {
    return inputFile(Path.of("file.txt"), fileContent);
  }

  public static InputFile inputFile(Path path, @Nullable String content) {
    return inputFile(path, content, null);
  }

  public static InputFile inputFile(Path path, @Nullable String content, @Nullable String language) {
    return inputFile(path, content, language, null);
  }

  public static InputFile inputFile(Path path, @Nullable String content, @Nullable String language, @Nullable InputFile.Type type) {
    TestInputFileBuilder builder = new TestInputFileBuilder(".", path.toString())
      .setType(InputFile.Type.MAIN)
      .setLanguage(language)
      .setCharset(UTF_8);
    if (content != null) {
      builder.setContents(content);
    }
    if (type != null) {
      builder.setType(type);
    }
    return builder.build();
  }

  public static InputFile inputFileFromPath(Path path) {
    return inputFileFromPath(path, InputFile.Type.MAIN);
  }

  public static InputFile inputFileFromPath(Path path, InputFile.Type type) {
    String content = null;
    try {
      content = Files.readString(path, UTF_8);
    } catch (IOException e) {
      // ignored, so InputFile.inputStream() will fail
    }
    return inputFile(path, content, null, type);
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

  private String[] allRuleKeys() {
    return Stream.of(secretCheckClassList(), textCheckClassList())
      .flatMap(Collection::stream)
      .map(checkClass -> {
        try {
          return ((Check) checkClass.getDeclaredConstructor().newInstance()).getRuleKey().toString();
        } catch (ReflectiveOperationException e) {
          throw new RuntimeException(e);
        }
      })
      .toArray(String[]::new);
  }

  public static InputFileContext inputFileContext(String fileContent) throws IOException {
    return inputFileContext(inputFile(fileContent));
  }

  public static InputFileContext inputFileContext(InputFile inputFile) throws IOException {
    var sensorContext = SensorContextTester.create(Path.of(".").toAbsolutePath());
    return new InputFileContext(sensorContext, inputFile);
  }

  public static SensorContextTester sensorContext(Check... checks) {
    return sensorContext(toRuleKeys(checks));
  }

  public static String[] toRuleKeys(Check... checks) {
    return Arrays.stream(checks).map(check -> check.getRuleKey().toString()).toArray(String[]::new);
  }

  public SensorContextTester defaultSensorContext() {
    return sensorContext(allRuleKeys());
  }

  public SensorContextTester sonarqubeSensorContext() {
    SensorContextTester sensorContextTester = sensorContext(allRuleKeys());
    sensorContextTester.setRuntime(SONARQUBE_RUNTIME);
    return sensorContextTester;
  }

  public static SensorContextTester sensorContext(String... activeRules) {
    return sensorContext(new File(".").getAbsoluteFile(), activeRules);
  }

  /**
   * By default, the SonarLint runtime is chosen because it allows to analyze all files and not only files assigned to a language
   */
  public static SensorContextTester sensorContext(File baseDir, String... activeRules) {
    return SensorContextTester.create(baseDir)
      .setRuntime(SONARQUBE_RUNTIME)
      .setActiveRules(activeRules(activeRules))
      .setSettings(createDefaultSettings());
  }

  private static MapSettings createDefaultSettings() {
    var mapSettings = new MapSettings();
    mapSettings.setProperty(TextAndSecretsSensor.TEXT_INCLUSIONS_KEY, "**/*.txt");
    return mapSettings;
  }

  public static DurationStatistics mockDurationStatistics() {
    var statistics = mock(DurationStatistics.class);
    when(statistics.timed(anyString(), any(Supplier.class)))
      .then(invocation -> invocation.getArgument(1, Supplier.class).get());
    Mockito.doAnswer((Answer<Void>) invocation -> {
      invocation.getArgument(1, Runnable.class).run();
      return null;
    }).when(statistics).timed(anyString(), any(Runnable.class));
    when(statistics.isRecordingEnabled()).thenReturn(true);
    return statistics;
  }

  public static AnalysisWarningsWrapper mockAnalysisWarning() {
    return new TestAnalysisWarningsWrapper();
  }

  public List<Class<?>> secretCheckClassList() {
    return new SecretsCheckList().checks();
  }

  public List<Class<?>> textCheckClassList() {
    return new TextCheckList().checks();
  }

  public static Set<SonarRuntime> sonarRuntimes() {
    return Set.of(SONARLINT_RUNTIME, SONARQUBE_RUNTIME, SONARCLOUD_RUNTIME);
  }
}
