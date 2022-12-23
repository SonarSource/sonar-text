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

public class GoogleCloudAccountKeyRuleTest {

  GoogleCloudAccountKeyRule underTest = new GoogleCloudAccountKeyRule();

  @Test
  public void testRuleProperties() {
    assertThat(underTest.getRuleKey()).isEqualTo("S6335");
    assertThat(underTest.getMessage()).isEqualTo("Make sure this Google Cloud service account key is not disclosed.");
  }

  @Test
  public void testRuleRegexPositive() throws Exception {
    List<Secret> secrets = underTest.findSecretsIn(TestUtils.readFileAndNormalize("src/test/files/google-cloud-account-key/GoogleCloudAccountPositive.json", UTF_8));

    assertThat(secrets)
      .extracting(Secret::getTextRange)
      .extracting("start.line", "start.lineOffset", "end.line", "end.lineOffset")
      .containsOnly(tuple(5, 18, 5, 1750));
  }

  @Test
  public void testRuleRegexNegative() throws Exception {
    List<Secret> secrets = underTest.findSecretsIn(TestUtils.readFileAndNormalize("src/test/files/google-cloud-account-key/GoogleCloudAccountNegative.json", UTF_8));

    assertThat(secrets).isEmpty();
  }
}
