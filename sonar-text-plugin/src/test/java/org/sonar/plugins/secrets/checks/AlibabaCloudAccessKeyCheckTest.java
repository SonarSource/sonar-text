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
import org.junit.jupiter.api.Test;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.plugins.common.Check;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.plugins.common.TestUtils.analyze;
import static org.sonar.plugins.common.TestUtils.inputFile;

class AlibabaCloudAccessKeyCheckTest {

  Check check = new AlibabaCloudAccessKeyCheck();

  @Test
  void key_id_positive() throws IOException {
    String fileContent = "LTAI5tBcc9SecYAomgyUSFs8";
    assertThat(analyze(check, fileContent)).containsExactly(
      "secrets:S6336 [1:0-1:24] Make sure this Alibaba Cloud Access Key ID is not disclosed.");
  }

  @Test
  void key_id_negative() throws IOException {
    String fileContent = "LNTTAI5tBcc9SecYAomgyUSFs8";
    assertThat(analyze(check, fileContent)).isEmpty();
  }

  @Test
  void key_secret_positive1() throws IOException {
    String fileContent = "String aliyunAccessKeySecret=\"KmkwlDrPBC68bgvZiNtrjonKIYmVT8\";";
    assertThat(analyze(check, fileContent)).containsExactly(
      "secrets:S6336 [1:30-1:60] Make sure this Alibaba Cloud Access Key Secret is not disclosed.");
  }

  @Test
  void key_secret_positive2() throws IOException {
    String fileContent = "static string AccessKeySecret = \"l0GdwcDYdJwB1VJ5pv0ormyTV9nhvW \";";
    assertThat(analyze(check, fileContent)).containsExactly(
      "secrets:S6336 [1:33-1:63] Make sure this Alibaba Cloud Access Key Secret is not disclosed.");
  }

  @Test
  void key_secret_positive3() throws IOException {
    String fileContent = "String aliyunAccessKeySecret=\"KmkwlDrPBC68bgvZiNtrjonKIYmVT8\";";
    assertThat(analyze(check, fileContent)).containsExactly(
            "secrets:S6336 [1:30-1:60] Make sure this Alibaba Cloud Access Key Secret is not disclosed.");
  }

  @Test
  void key_secret_negative() throws Exception {
    InputFile file = inputFile(Path.of("src", "test", "resources", "checks", "GoogleCloudAccountKeyCheck", "GoogleCloudAccountNegative.json"));
    assertThat(analyze(check, file)).isEmpty();
  }

  @Test
  void key_secret_negative_low_entropy() throws IOException {
    String fileContent = "String aliyunAccessKeySecret=\"100000000000000000000000000000\";";
    assertThat(analyze(check, fileContent)).isEmpty();
  }

}
