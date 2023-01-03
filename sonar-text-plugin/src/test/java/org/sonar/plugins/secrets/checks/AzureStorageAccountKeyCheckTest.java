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
import java.util.Collection;
import org.junit.jupiter.api.Test;
import org.sonar.api.batch.sensor.issue.Issue;
import org.sonar.plugins.common.Check;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.plugins.common.TestUtils.analyze;
import static org.sonar.plugins.common.TestUtils.asString;
import static org.sonar.plugins.common.TestUtils.inputFile;

class AzureStorageAccountKeyCheckTest {

  Check check = new AzureStorageAccountKeyCheck();

  @Test
  void testRuleFirstRegexPositive() throws IOException {
    Collection<Issue> issues = analyze(check, inputFile("async function main() {\n" +
      "  const account = process.env.ACCOUNT_NAME || \"accountname\";\n" +
      "  const accountKey = process.env.ACCOUNT_KEY || \"4dVw+l0W8My+FwuZ08dWXn+gHxcmBtS7esLAQSrm6/Om3jeyUKKGMkfAh38kWZlItThQYsg31v23A0w/uVP4pg==\";\n" +
      "  const sharedKeyCredential = new StorageSharedKeyCredential(account, accountKey);\n" +
      "  const blobServiceClient = new BlobServiceClient(\n" +
      "    `https://${account}.blob.core.windows.net`,\n" +
      "    sharedKeyCredential\n" +
      "  );\n" +
      "}"));
    assertThat(asString(issues)).containsExactly(
      "secrets:S6338 [3:49-3:137] Make sure this Azure Storage Account Key is not disclosed.");
  }

  @Test
  void testRuleSecondRegexPositive() throws IOException {
    Collection<Issue> issues = analyze(check, inputFile(
      "const connStr = \"DefaultEndpointsProtocol=https;AccountName=testaccountname;AccountKey=4dVw+l0W8My+FwuZ08dWXn+gHxcmBtS7esLAQSrm6/Om3jeyUKKGMkfAh38kWZlItThQYsg31v23A0w/uVP4pg==\";"));
    assertThat(asString(issues)).containsExactly(
      "secrets:S6338 [1:87-1:175] Make sure this Azure Storage Account Key is not disclosed.");
  }

  @Test
  void testRuleSecondRegexPositiveEvenWhenCoreWindowsNetStringPresent() throws IOException {
    Collection<Issue> issues = analyze(check, inputFile(
      "const connStr = \"DefaultEndpointsProtocol=https;AccountName=testaccountname;AccountKey=4dVw+l0W8My+FwuZ08dWXn+gHxcmBtS7esLAQSrm6/Om3jeyUKKGMkfAh38kWZlItThQYsg31v23A0w/uVP4pg==;EndpointSuffix=core.windows.net\";"));
    assertThat(asString(issues)).containsExactly(
      "secrets:S6338 [1:87-1:175] Make sure this Azure Storage Account Key is not disclosed.");
  }

  @Test
  void testRuleRegexNegative() throws IOException {
    Collection<Issue> issues = analyze(check, inputFile(
      "AccountKey=BtS7esLAQSrm6/Om3jeyUKKGMkfAh38kWZlItThQYsg31v23A0w/uVP4pg==\";"));
    assertThat(issues).isEmpty();
  }

}
