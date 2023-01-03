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
package org.sonar.plugins.secrets.checks;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import org.junit.jupiter.api.Test;
import org.sonar.api.batch.sensor.issue.Issue;
import org.sonar.plugins.common.Check;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.plugins.common.TestUtils.analyze;
import static org.sonar.plugins.common.TestUtils.asString;
import static org.sonar.plugins.common.TestUtils.inputFile;

class AlibabaCloudAccessKeyCheckTest {

  Check check = new AlibabaCloudAccessKeyCheck();

  @Test
  void testAlibabaCloudAccessKeyIDsPositive() throws IOException {
    Collection<Issue> issues = analyze(check, inputFile("LTAI5tBcc9SecYAomgyUSFs8"));
    assertThat(asString(issues)).containsExactly(
      "secrets:S6336 [1:0-1:24] Make sure this Alibaba Cloud Access Key ID is not disclosed.");
  }

  @Test
  void testAlibabaCloudAccessKeyIDsNegative() throws IOException {
    Collection<Issue> issues = analyze(check, inputFile("LNTTAI5tBcc9SecYAomgyUSFs8"));
    assertThat(issues).isEmpty();
  }

  @Test
  void testAlibabaCloudAccessKeySecrets1Positive() throws IOException {
    Collection<Issue> issues = analyze(check, inputFile("String aliyunAccessKeySecret=\"KmkwlDrPBC68bgvZiNtrjonKIYmVT8\";"));
    assertThat(asString(issues)).containsExactly(
      "secrets:S6336 [1:30-1:60] Make sure this Alibaba Cloud Access Key Secret is not disclosed.");
  }

  @Test
  void testAlibabaCloudAccessKeySecrets2Positive() throws IOException {
    Collection<Issue> issues = analyze(check, inputFile("static string AccessKeySecret = \"l0GdwcDYdJwB1VJ5pv0ormyTV9nhvW \";"));
    assertThat(asString(issues)).containsExactly(
      "secrets:S6336 [1:33-1:63] Make sure this Alibaba Cloud Access Key Secret is not disclosed.");
  }

  @Test
  void testAlibabaCloudAccessKeySecretsNegative() throws Exception {
    Collection<Issue> issues = analyze(check, inputFile(Path.of("src", "test", "files", "google-cloud-account-key", "GoogleCloudAccountNegative.json"), UTF_8));
    assertThat(issues).isEmpty();
  }

  @Test
  void testAlibabaCloudAccessKeySecretsExamplePositive() throws IOException {
    Collection<Issue> issues = analyze(check, inputFile("String aliyunAccessKeySecret=\"KmkwlDrPBC68bgvZiNtrjonKIYmVT8\";"));
    assertThat(asString(issues)).containsExactly(
      "secrets:S6336 [1:30-1:60] Make sure this Alibaba Cloud Access Key Secret is not disclosed.");
  }

  @Test
  void testAlibabaCloudAccessKeySecretsExampleNegativeLowEntropy() throws IOException {
    Collection<Issue> issues = analyze(check, inputFile("String aliyunAccessKeySecret=\"100000000000000000000000000000\";"));
    assertThat(issues).isEmpty();
  }

}
