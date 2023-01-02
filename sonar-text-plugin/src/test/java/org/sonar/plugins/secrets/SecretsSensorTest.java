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
package org.sonar.plugins.secrets;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.rule.internal.ActiveRulesBuilder;
import org.sonar.api.batch.rule.internal.NewActiveRule;
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.batch.sensor.issue.IIssue;
import org.sonar.api.rule.RuleKey;
import org.sonar.plugins.common.TextAndSecretsSensor;
import org.sonar.plugins.secrets.rules.SecretCheckList;
import org.sonar.plugins.secrets.rules.SecretRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class SecretsSensorTest {

  TextAndSecretsSensor underTest;

  @Rule
  public TemporaryFolder tmp = new TemporaryFolder();
  SensorContextTester context;
  Path path;

  @Before
  public void init() throws IOException {
    path = tmp.newFolder().toPath();
    context = SensorContextTester.create(this.path);

    ActiveRulesBuilder activeRules = new ActiveRulesBuilder();
    Arrays.asList("S6290", "S6292", "S6334", "S6335", "S6336", "S6337", "S6338").stream()
            .map(key -> new NewActiveRule.Builder().setRuleKey(RuleKey.of("secrets", key)).build())
            .forEach(activeRules::addRule);
    context.setActiveRules(activeRules.build());

    CheckFactory checkFactory = new CheckFactory(context.activeRules());

    underTest = new TextAndSecretsSensor(checkFactory);
  }

  @Test
  public void describeTest() {
    DefaultSensorDescriptor descriptor = new DefaultSensorDescriptor();
    underTest.describe(descriptor);

    assertThat(descriptor.name()).isEqualTo("TextAndSecretsSensor");
    assertThat(descriptor.isProcessesFilesIndependently()).isTrue();
    assertThat(descriptor.ruleRepositories()).containsExactlyInAnyOrder("text", "secrets");
  }

  @Test
  public void should_raise_an_issue_when_a_secret_is_detected() throws IOException {
    File baseDir = new File("src/test/files/google-cloud-account-key/").getAbsoluteFile();
    SensorContextTester context = SensorContextTester.create(baseDir);
    context.fileSystem().add(new TestInputFileBuilder("moduleKey", "GoogleCloudAccountPositive.json")
      .setModuleBaseDir(baseDir.toPath())
      .setCharset(StandardCharsets.UTF_8)
      .initMetadata(new String(Files.readAllBytes(new File(baseDir, "GoogleCloudAccountPositive.json").toPath()), StandardCharsets.UTF_8))
      .build());
    ActiveRulesBuilder activeRulesBuilder = new ActiveRulesBuilder();
    SecretCheckList.createInstances().stream()
      .map(SecretRule::getRuleKey)
      .distinct()
      .map(ruleKey -> RuleKey.of("secrets", ruleKey))
      .forEach(ruleKey -> activeRulesBuilder.addRule(new NewActiveRule.Builder().setRuleKey(ruleKey).build()));

    context.setActiveRules(activeRulesBuilder.build());

    underTest.execute(context);

    assertThat(context.allIssues())
      .extracting(IIssue::ruleKey, issue -> issue.primaryLocation().textRange().start().line(), issue -> issue.primaryLocation().message())
      .containsOnly(tuple(RuleKey.of("secrets", "S6335"), 5, "Make sure this Google Cloud service account key is not disclosed."));
  }

  @Test
  public void should_not_raise_an_issue_when_no_rule_is_active() throws IOException {
    File baseDir = new File("src/test/files/google-cloud-account-key/").getAbsoluteFile();
    SensorContextTester context = SensorContextTester.create(baseDir);
    context.fileSystem().add(new TestInputFileBuilder("moduleKey", "GoogleCloudAccountPositive.json")
            .setModuleBaseDir(baseDir.toPath())
            .setCharset(StandardCharsets.UTF_8)
            .initMetadata(new String(Files.readAllBytes(new File(baseDir, "GoogleCloudAccountPositive.json").toPath()), StandardCharsets.UTF_8))
            .build());
    context.setActiveRules(new ActiveRulesBuilder().build());

    underTest.execute(context);

    assertThat(context.allIssues()).isEmpty();
  }

  @Test
  public void should_not_raise_an_issue_or_error_when_the_input_file_does_not_exist() {
    SensorContextTester context = SensorContextTester.create(new File("src/test/files/").getAbsoluteFile());
    context.fileSystem().add(new TestInputFileBuilder("moduleKey", "Missing.java")
      .build());

    underTest.execute(context);

    assertThat(context.allIssues()).isEmpty();
  }

}
