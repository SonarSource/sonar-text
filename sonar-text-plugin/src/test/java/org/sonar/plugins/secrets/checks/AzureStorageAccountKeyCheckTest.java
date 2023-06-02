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
package org.sonar.plugins.secrets.checks;

import java.io.IOException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.sonar.plugins.common.Check;
import org.sonar.plugins.secrets.api.SpecificationBasedCheck;
import org.sonar.plugins.secrets.api.SpecificationLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.plugins.common.TestUtils.analyze;

class AzureStorageAccountKeyCheckTest {

  static Check check;

  @BeforeAll
  public static void init() {
    check = new AzureStorageAccountKeyCheck();
    SpecificationLoader specificationLoader = new SpecificationLoader();
    ((SpecificationBasedCheck) check).initialize(specificationLoader);
  }

  @Test
  void account_key_positive1() throws IOException {
    String fileContent = "" +
      "async function main() {\n" +
      "  const account = process.env.ACCOUNT_NAME || \"accountname\";\n" +
      "  const accountKey = process.env.ACCOUNT_KEY || \"4dVw+l0W8My+FwuZ08dWXn+gHxcmBtS7esLAQSrm6/Om3jeyUKKGMkfAh38kWZlItThQYsg31v23A0w" +
      "/uVP4pg==\";\n" +
      "  const sharedKeyCredential = new StorageSharedKeyCredential(account, accountKey);\n" +
      "  const blobServiceClient = new BlobServiceClient(\n" +
      "    `https://${account}.blob.core.windows.net`,\n" +
      "    sharedKeyCredential\n" +
      "  );\n" +
      "}";
    assertThat(analyze(check, fileContent)).containsExactly(
      "secrets:S6338 [3:49-3:137] Make sure this Azure Storage Account Key gets revoked, changed, and removed from the code.");
  }

  @Test
  void account_key_positive2() throws IOException {
    String fileContent = "const connStr = \"DefaultEndpointsProtocol=https;AccountName=testaccountname;" +
      "AccountKey=4dVw+l0W8My+FwuZ08dWXn+gHxcmBtS7esLAQSrm6/Om3jeyUKKGMkfAh38kWZlItThQYsg31v23A0w/uVP4pg==\";";
    assertThat(analyze(check, fileContent)).containsExactly(
      "secrets:S6338 [1:87-1:175] Make sure this Azure Storage Account Key gets revoked, changed, and removed from the code.");
  }

  @Test
  void account_key_positive_even_when_core_windows_net_is_present() throws IOException {
    String fileContent = "const connStr = \"DefaultEndpointsProtocol=https;AccountName=testaccountname;" +
      "AccountKey=4dVw+l0W8My+FwuZ08dWXn+gHxcmBtS7esLAQSrm6/Om3jeyUKKGMkfAh38kWZlItThQYsg31v23A0w/uVP4pg==;EndpointSuffix=core.windows" +
      ".net\";";
    assertThat(analyze(check, fileContent)).containsExactly(
      "secrets:S6338 [1:87-1:175] Make sure this Azure Storage Account Key gets revoked, changed, and removed from the code.");
  }

  @Test
  void account_key_negative() throws IOException {
    String fileContent = "AccountKey=BtS7esLAQSrm6/Om3jeyUKKGMkfAh38kWZlItThQYsg31v23A0w/uVP4pg==\";";
    assertThat(analyze(check, fileContent)).isEmpty();
  }

}
