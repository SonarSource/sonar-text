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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.sonar.plugins.secrets.TestUtils.aNormalizedInputFile;

public class AlibabaCloudAccessKeySecretsRuleTest {

  AlibabaCloudAccessKeySecretsRule underTest = new AlibabaCloudAccessKeySecretsRule();

  @Test
  public void testRuleProperties() {
    assertThat(underTest.getRuleKey()).isEqualTo("S6336");
    assertThat(underTest.getMessage()).isEqualTo("Make sure this Alibaba Cloud Access Key Secret is not disclosed.");
  }

  @Test
  public void testRuleRegex1Positive() {
    List<Secret> secrets = underTest.findSecretsIn(TestUtils.aNormalizedInputFile("String aliyunAccessKeySecret=\"KmkwlDrPBC68bgvZiNtrjonKIYmVT8\";"));

    assertThat(secrets)
      .extracting(Secret::getTextRange)
      .extracting("start.line", "start.lineOffset", "end.line", "end.lineOffset")
      .containsOnly(tuple(1, 30, 1, 60));
  }

  @Test
  public void testRuleRegex2Positive() {
    List<Secret> secrets = underTest.findSecretsIn(TestUtils.aNormalizedInputFile("static string AccessKeySecret = \"l0GdwcDYdJwB1VJ5pv0ormyTV9nhvW \";"));

    assertThat(secrets)
      .extracting(Secret::getTextRange)
      .extracting("start.line", "start.lineOffset", "end.line", "end.lineOffset")
      .containsOnly(tuple(1, 33, 1, 63));
  }

  @Test
  public void testRuleRegexNegative() throws Exception {
    List<Secret> secrets = underTest.findSecretsIn(TestUtils.readFileAndNormalize("src/test/files/google-cloud-account-key/GoogleCloudAccountNegative.json", UTF_8));

    assertThat(secrets).isEmpty();
  }

  @Test
  public void testRuleRegexExamplePositive() {
    List<Secret> secrets = underTest.findSecretsIn(TestUtils.aNormalizedInputFile("String aliyunAccessKeySecret=\"KmkwlDrPBC68bgvZiNtrjonKIYmVT8\";"));

    assertThat(secrets)
      .extracting(Secret::getTextRange)
      .extracting("start.line", "start.lineOffset", "end.line", "end.lineOffset")
      .containsOnly(tuple(1, 30, 1, 60));
  }

  @Test
  public void testRuleRegexExampleNegativeLowEntropy() {
    List<Secret> secrets = underTest.findSecretsIn(TestUtils.aNormalizedInputFile("String aliyunAccessKeySecret=\"100000000000000000000000000000\";"));

    assertThat(secrets).isEmpty();
  }

}
