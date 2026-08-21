/*
 * SonarQube Text Plugin
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.slf4j.event.Level;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.rule.RuleKey;
import org.sonar.plugins.common.git.GitService;
import org.sonar.plugins.common.measures.TelemetryReporter;
import org.sonar.plugins.common.warnings.AnalysisWarningsWrapper;
import org.sonar.plugins.secrets.configuration.SecretsSpecificationContainer;
import org.sonar.plugins.secrets.utils.CheckContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.plugins.common.TestUtils.SONARCLOUD_RUNTIME;
import static org.sonar.plugins.common.TestUtils.SONARQUBE_RUNTIME;
import static org.sonar.plugins.common.TestUtils.activeRules;
import static org.sonar.plugins.common.TestUtils.inputFile;
import static org.sonar.plugins.common.TestUtils.sensorContext;
import static org.sonar.plugins.common.TestUtils.toRuleKeys;

class TextAndSecretsSensorTest extends AbstractTextAndSecretsSensorTest {

  private static final TestUtils TEST_UTILS = new TestUtils();
  /** One of the rules that are rolled out gradually on SonarQube Cloud, see {@code SONARQUBE_CLOUD_ROLLOUT_FLAGS}. */
  private static final String ROLLED_OUT_RULE_KEY = "secrets:S8135";
  private static final String ROLLED_OUT_RULE_KEY_S8136 = "secrets:S8136";
  private static final String ROLLOUT_FEATURE_FLAG_KEY_S8135 = TextAndSecretsSensor.FEATURE_FLAG_PROPERTY_PREFIX + "cloud-security-enable-secrets-S8135";
  private static final String ROLLOUT_FEATURE_FLAG_KEY_S8136 = TextAndSecretsSensor.FEATURE_FLAG_PROPERTY_PREFIX + "cloud-security-enable-secrets-S8136";
  private static final String ROLLOUT_FLAG_TELEMETRY_KEY_S8135 = TelemetryReporter.KEY_PREFIX + "secrets.rollout_flag.s8135";
  private static final String ROLLOUT_FLAG_TELEMETRY_KEY_S8136 = TelemetryReporter.KEY_PREFIX + "secrets.rollout_flag.s8136";

  @Override
  protected TextAndSecretsSensor sensor(Check... checks) {
    CheckFactory checkFactory = new CheckFactory(activeRules(toRuleKeys(checks)));
    return new TextAndSecretsSensor(SONARQUBE_RUNTIME, checkFactory, new SecretsSpecificationContainer(), new CheckContainer()) {
      @Override
      protected List<Check> getActiveChecks() {
        return Arrays.stream(checks).toList();
      }
    };
  }

  @Override
  protected TextAndSecretsSensor sensor(SensorContext sensorContext) {
    return new TextAndSecretsSensor(sensorContext.runtime(), new CheckFactory(sensorContext.activeRules()), new SecretsSpecificationContainer(),
      new CheckContainer());
  }

  @Override
  protected TestUtils testUtils() {
    return TEST_UTILS;
  }

  @Override
  protected String sensorName() {
    return "TextAndSecretsSensor";
  }

  @Test
  void shouldRaiseAnalysisWarningWhenDemoModeIsEnabled() {
    var context = testUtils().defaultSensorContext();
    context.settings().setProperty(TextAndSecretsSensor.DEMO_MODE_KEY, "true");
    var analysisWarnings = new TestAnalysisWarningsWrapper();

    sensorWithAnalysisWarnings(analysisWarnings, new ReportIssueAtLineOneCheck()).execute(context);

    assertThat(analysisWarnings.getWarnings())
      .anyMatch(warning -> warning.contains("Demo mode is enabled through the property \"sonar.secrets.demoMode\""));
  }

  @Test
  void shouldNotRaiseAnalysisWarningWhenDemoModeIsDisabled() {
    var context = testUtils().defaultSensorContext();
    var analysisWarnings = new TestAnalysisWarningsWrapper();

    sensorWithAnalysisWarnings(analysisWarnings, new ReportIssueAtLineOneCheck()).execute(context);

    assertThat(analysisWarnings.getWarnings()).noneMatch(warning -> warning.contains("Demo mode is enabled"));
  }

  private static TextAndSecretsSensor sensorWithAnalysisWarnings(AnalysisWarningsWrapper analysisWarnings, Check... checks) {
    CheckFactory checkFactory = new CheckFactory(activeRules(toRuleKeys(checks)));
    return new TextAndSecretsSensor(SONARQUBE_RUNTIME, checkFactory, analysisWarnings, new SecretsSpecificationContainer(), new CheckContainer()) {
      @Override
      protected List<Check> getActiveChecks() {
        return Arrays.stream(checks).toList();
      }
    };
  }

  @Test
  void shouldResolvePluginVersionFromClasspathResource() {
    assertThat(TextAndSecretsSensor.resolvePluginVersion(getClass().getClassLoader())).isEqualTo("1.2.3-test");
  }

  @Test
  void shouldFallBackToUnknownPluginVersionWhenResourceIsMissing() {
    ClassLoader emptyClassLoader = new URLClassLoader(new URL[0], null);
    assertThat(TextAndSecretsSensor.resolvePluginVersion(emptyClassLoader)).isEqualTo("unknown");
  }

  @Test
  void shouldFallBackToUnknownPluginVersionWhenResourceCannotBeRead() {
    ClassLoader brokenClassLoader = new ClassLoader() {
      @Override
      public InputStream getResourceAsStream(String name) {
        return new InputStream() {
          @Override
          public int read() throws IOException {
            throw new IOException("boom");
          }
        };
      }
    };
    assertThat(TextAndSecretsSensor.resolvePluginVersion(brokenClassLoader)).isEqualTo("unknown");
  }

  @Test
  void shouldFallBackToUnknownPluginVersionWhenPlaceholderWasNotSubstituted() {
    ClassLoader unsubstitutedClassLoader = new ClassLoader() {
      @Override
      public InputStream getResourceAsStream(String name) {
        return new ByteArrayInputStream("plugin.version=${version}".getBytes(StandardCharsets.UTF_8));
      }
    };
    assertThat(TextAndSecretsSensor.resolvePluginVersion(unsubstitutedClassLoader)).isEqualTo("unknown");
  }

  @Test
  void shouldNotRunChecksOfRulesWhoseSonarQubeCloudRolloutFlagIsOff() {
    var rolledOutCheck = checkForRule(ROLLED_OUT_RULE_KEY);
    var regularCheck = new ReportIssueAtLineOneCheck();
    var context = spy(sensorContext(rolledOutCheck, regularCheck));
    context.setRuntime(SONARCLOUD_RUNTIME);

    analyse(sensor(rolledOutCheck, regularCheck), context, inputFile(Path.of("a.txt"), "foo", "secrets"));

    assertThat(context.allIssues()).extracting(issue -> issue.ruleKey().toString())
      .containsExactly(regularCheck.getRuleKey().toString());
    assertThat(logTester.logs(Level.INFO)).contains("The following rules are activated in the quality profile but will not raise any issue, " +
      "because they are being rolled out progressively: " + ROLLED_OUT_RULE_KEY + ". " +
      "You can enable it by setting: " + ROLLOUT_FEATURE_FLAG_KEY_S8135 + " to true.");
    verify(context).addTelemetryProperty(ROLLOUT_FLAG_TELEMETRY_KEY_S8135, "0");
    verify(context).addTelemetryProperty(ROLLOUT_FLAG_TELEMETRY_KEY_S8136, "0");
  }

  @Test
  void shouldListEveryDisabledRuleAndItsFeatureFlagWhenAllRolloutFlagsAreOff() {
    var checkS8135 = checkForRule(ROLLED_OUT_RULE_KEY);
    var checkS8136 = checkForRule(ROLLED_OUT_RULE_KEY_S8136);
    var context = spy(sensorContext(checkS8135, checkS8136));
    context.setRuntime(SONARCLOUD_RUNTIME);

    analyse(sensor(checkS8135, checkS8136), context, inputFile(Path.of("a.txt"), "foo", "secrets"));

    assertThat(context.allIssues()).isEmpty();
    assertThat(logTester.logs(Level.INFO)).contains("The following rules are activated in the quality profile but will not raise any issue, " +
      "because they are being rolled out progressively: " + ROLLED_OUT_RULE_KEY + ", " + ROLLED_OUT_RULE_KEY_S8136 + ". " +
      "You can enable it by setting: " + ROLLOUT_FEATURE_FLAG_KEY_S8135 + ", " + ROLLOUT_FEATURE_FLAG_KEY_S8136 + " to true.");
    // the telemetry is flushed even though every active check is gated off and nothing is analyzed
    verify(context).addTelemetryProperty(ROLLOUT_FLAG_TELEMETRY_KEY_S8135, "0");
    verify(context).addTelemetryProperty(ROLLOUT_FLAG_TELEMETRY_KEY_S8136, "0");
  }

  @Test
  void shouldRunChecksOfRulesWhoseSonarQubeCloudRolloutFlagIsOn() {
    var rolledOutCheck = checkForRule(ROLLED_OUT_RULE_KEY);
    var context = spy(sensorContext(rolledOutCheck));
    context.setRuntime(SONARCLOUD_RUNTIME);
    context.setSettings(context.settings().setProperty(ROLLOUT_FEATURE_FLAG_KEY_S8135, "true"));

    analyse(sensor(rolledOutCheck), context, inputFile(Path.of("a.txt"), "foo", "secrets"));

    assertThat(context.allIssues()).extracting(issue -> issue.ruleKey().toString()).containsExactly(ROLLED_OUT_RULE_KEY);
    assertThat(logTester.logs(Level.INFO)).noneMatch(log -> log.contains("rolled out progressively"));
    verify(context).addTelemetryProperty(ROLLOUT_FLAG_TELEMETRY_KEY_S8135, "1");
    verify(context).addTelemetryProperty(ROLLOUT_FLAG_TELEMETRY_KEY_S8136, "0");
  }

  @Test
  void shouldNotGateRulesBehindRolloutFlagsOutsideOfSonarQubeCloud() {
    var rolledOutCheck = checkForRule(ROLLED_OUT_RULE_KEY);
    var context = spy(sensorContext(rolledOutCheck));

    analyse(sensor(rolledOutCheck), context, inputFile(Path.of("a.txt"), "foo", "secrets"));

    assertThat(context.allIssues()).extracting(issue -> issue.ruleKey().toString()).containsExactly(ROLLED_OUT_RULE_KEY);
    verify(context, never()).addTelemetryProperty(argThat(key -> key.contains("rollout_flag")), any());
  }

  @Test
  void shouldLogDebugMessageWhenTheGitServiceCannotBeClosed() throws Exception {
    logTester.setLevel(Level.DEBUG);
    var check = new ReportIssueAtLineOneCheck();
    var context = sensorContext(check);
    var sensor = spy(sensor(check));
    var gitService = mock(GitService.class);
    when(gitService.retrieveDirtyFileNames()).thenReturn(new GitService.DirtyFileNamesResult(true, Set.of()));
    doThrow(new IllegalStateException("Cannot close")).when(gitService).close();
    when(sensor.createGitService(any())).thenReturn(gitService);

    analyse(sensor, context, inputFile(Path.of("a.txt"), "foo", "secrets"));

    assertThat(logTester.logs(Level.DEBUG)).contains("Error closing GitService");
  }

  private static Check checkForRule(String ruleKey) {
    var check = new ReportIssueAtLineOneCheck();
    check.setRuleKey(RuleKey.parse(ruleKey));
    return check;
  }
}
