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
package org.sonar.plugins.secrets.rules;

import java.util.List;
import org.junit.Test;
import org.sonar.plugins.secrets.TestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.sonar.plugins.secrets.TestUtils.aNormalizedInputFile;

public class AwsAccessKeyRuleTest {

  AwsAccessKeyRule underTest = new AwsAccessKeyRule();

  @Test
  public void testRuleProperties() {
    assertThat(underTest.getRuleKey()).isEqualTo("S6290");
  }

  @Test
  public void testRuleFirstRegexPositive() {
    List<Secret> secrets = underTest.findSecretsIn(TestUtils.aNormalizedInputFile("var creds = new AWS.Credentials({ " +
      "     secretAccessKey: 'kHeUAwnSUizTWpSbyGAz4f+As5LshPIjvtpswqGb' " +
      "});"));

    assertThat(secrets)
      .extracting(Secret::getTextRange)
      .extracting("start.line", "start.lineOffset", "end.line", "end.lineOffset")
      .containsOnly(tuple(1, 57, 1, 97));
  }

  @Test
  public void testRuleSecondRegexPositive() {
    List<Secret> secrets = underTest.findSecretsIn(TestUtils.aNormalizedInputFile("aws_secret_access_key=kHeUAwnSUizTWpSbyGAz4f+As5LshPIjvtpswqGb"));

    assertThat(secrets)
      .extracting(Secret::getTextRange)
      .extracting("start.line", "start.lineOffset", "end.line", "end.lineOffset")
      .containsOnly(tuple(1, 22, 1, 62));
  }

  @Test
  public void testRuleRegexNegative() {
    List<Secret> secrets = underTest.findSecretsIn(TestUtils.aNormalizedInputFile("public class Foo {\n"
      + "  public static final String KEY = \"AKIGKECZXA7AEIJLMQ\"\n"
      + "}"));

    assertThat(secrets).isEmpty();
  }
}
