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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.sonar.plugins.common.Check;
import org.sonar.plugins.secrets.api.SpecificationBasedCheck;
import org.sonar.plugins.secrets.api.SpecificationLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.plugins.common.TestUtils.analyze;

@java.lang.SuppressWarnings("squid:S6290")
class AwsCheckTest {

  static Check check;

  @BeforeAll
  public static void init() {
    check = new AwsCheck();
    SpecificationLoader specificationLoader = new SpecificationLoader();
    ((SpecificationBasedCheck) check).initialize(specificationLoader);
  }

  @Test
  void key_id_positive() throws IOException {
    String fileContent = "" +
      "public class Foo {\n" +
      "  public static final String KEY = \"AKIAIGKECZXA7AEIJLMQ\"\n" +
      "}";
    assertThat(analyze(check, fileContent)).isEmpty();
  }

  @Test
  void key_id_positive_not_example() throws IOException {
    String fileContent = "" +
      "public class Foo {\n" +
      "  /* This is for aws */\n" +
      "  public static final String KEY = \"AKIAIGKECZXA7EXAMPLF\"\n" +
      "}";
    assertThat(analyze(check, fileContent)).containsExactly(
      "secrets:S6290 [3:36-3:56] Make sure this AWS Access Key ID gets revoked, changed, and removed from the code.");
  }

  @Test
  void access_key_positive1() throws IOException {
    String fileContent = "" +
      "var creds = new AWS.Credentials({ " +
      "     secretAccessKey: 'kHeUAwnSUizTWpSbyGAz4f+As5LshPIjvtpswqGb' " +
      "});";
    assertThat(analyze(check, fileContent)).containsExactly(
      "secrets:S6290 [1:57-1:97] Make sure this AWS Secret Access Key gets revoked, changed, and removed from the code.");
  }

  @Test
  void access_key_positive2() throws IOException {
    String fileContent = "aws_secret_access_key=kHeUAwnSUizTWpSbyGAz4f+As5LshPIjvtpswqGb";
    assertThat(analyze(check, fileContent)).containsExactly(
      "secrets:S6290 [1:22-1:62] Make sure this AWS Secret Access Key gets revoked, changed, and removed from the code.");
  }

  @Test
  void session_token_positive() throws IOException {
    String fileContent = "AWS_SESSION_TOKEN=IQoJb3JpZ2luX2VjEKL//////////wE" +
      "aDGV1LWNlbnRyYWwtMSJHMEUCIQDFlDUEvUa6slxlkKKn8zbLkN/j1f7lKJdXJ03PQ5T5ZwIgDYlshciO8nyfnmjUfFy4I2+rEuPHBe" +
      "xsvfBo3MlCdgQqugMISxAAGgw4NTk4OTY2NzUzMDYiDFKPV7D/QmnqFWRYpiqXAypJf6TksPZXImVpIUU0Yj0uJhNN0o/HcO8hfQ4BX" +
      "uCvpm1DOiVsH6VXMxgNdpGTWr8CjNpEt/eYwSk6MAVPOtjg5+lY2qoGJrUuxwhiKe+BquVM17h0giZ18h1B4ozDGkfxA/vGSJa/qBzn" +
      "F0yEpLE+fJoesGe4ZpATs8oUN94/XkrL/eYzXsW3ZD1ZX66QzmSFHhgTJc24d9bezGjR32fEJD/dBm9La+7wpc4+jrXCmt6yxHox0gC" +
      "uGrSagcJfPh9pVYneM81fnD/S7Kicb1Pw8MiChfqW0hao1twr4wMgp9N3JlYQNK3fZKbMU/qlvoKTz8D0Joa4elSp4rU4reVUsujCXV" +
      "E95PDyj4LD3IDXHF5SAd/23/M/IucMRyeWlRE4pCtry68ENpojXr0tdyyVs8XSkgCGgup/BqDTkBnEBD+V5hOIrHJv5rJ6KpaxEZG0o" +
      "zUJdaUpCseSSKK4Jn7liqVqF5EzOOXelqTAACcJmILKQHqke8n3imNs72oi8tu1N+oqbFp60K9whtLDm0JZSavpmRDkMODb8/4FOusB" +
      "HFYZCuxMUmotN9Dkzp4InT7kJdKZ/kr61SMhU4hj7vTdjhcRHItO2P+jR7+38kQLDR4O1HR1XkHzLMwDvDwZULeOl6afS1ZpbO8XpeP" +
      "HaaLnEqJeZ8BpnfwBEiylK3HGzGAP7WcAgFlMO9AEqoGnnbUBFcL+IYnZ3JFPy0sGsrH4cOC8Gxy2icQKrGpdIyMqGjb2hZsSc1S4nj" +
      "GpK0AlCEKrAjzpr6SzPSwLnFtAJpztHbgb9Z7D2jdsjugQYdFwi6/9GKOI/slKqt5/vb7dLnSyeAY+jTaoveUZf6D5yM8PCKrvw5/k+" +
      "A1XJw==";
    assertThat(analyze(check, fileContent)).containsExactly(
      "secrets:S6290 [1:18-1:1086] Make sure this AWS Session token gets revoked, changed, and removed from the code.");
  }

  @ParameterizedTest
  @ValueSource(strings = {
    // extra characters before
    "public static final String KEY = \"BACAKIAIGKECZXA7AEIJLMQ\"; // Not really AWS\n",
    "\tsecretKey := stellar1.SecretKey(\"SDGCPMBQHYAIWM3PQOEKWICDMLVT7REJ24J26QEYJYGB6FJ\")",
    // extra characters after
    "public static final String KEY = \"AKIAIGKECZXA7AEIJLMQBAC\";\n",
    "\tsecretKey := stellar1.SecretKey(\"QHYAIWM3PQOEKWICDMLVT7REJ24J26QEYJYGB6FJRPTKDULQX\")",
    // not a key id
    "public class Foo {\n" +
      "  public static final String KEY = \"AKIGIGKECZXA7AEIJLMQ\"\n" +
      "}",
    // key id with EXAMPLE
    "public class Foo {\n" +
      "  public static final String KEY = \"AKIAIGKECZXA7EXAMPLE\"\n" +
      "}",
    // not an access key
    "public class Foo {\n" +
      "  public static final String KEY = \"AKIGKECZXA7AEIJLMQ\"\n" +
      "}",
    // not a session token
    "AWS_SESS_TOK=IQoJb3JpZ2luX2VjEKL//////////wEaDGV1" +
      "LWNlbnRyYWwtMSJHMEUCIQDFlDUEvUa6slxlkKKn8zbLkN/j1f7lKJdXJ03PQ5T5ZwIgDYlshciO8nyfnmjUfFy4I2+rEuPHBexsvfB" +
      "o3MlCdgQqugMISxAAGgw4NTk4OTY2NzUzMDYiDFKPV7D/QmnqFWRYpiqXAypJf6TksPZXImVpIUU0Yj0uJhNN0o/HcO8hfQ4BXuCvpm" +
      "1DOiVsH6VXMxgNdpGTWr8CjNpEt/eYwSk6MAVPOtjg5+lY2qoGJrUuxwhiKe+BquVM17h0giZ18h1B4ozDGkfxA/vGSJa/qBznF0yEp" +
      "LE+fJoesGe4ZpATs8oUN94/XkrL/eYzXsW3ZD1ZX66QzmSFHhgTJc24d9bezGjR32fEJD/dBm9La+7wpc4+jrXCmt6yxHox0gCuGrSa" +
      "gcJfPh9pVYneM81fnD/S7Kicb1Pw8MiChfqW0hao1twr4wMgp9N3JlYQNK3fZKbMU/qlvoKTz8D0Joa4elSp4rU4reVUsujCXVE95PD" +
      "yj4LD3IDXHF5SAd/23/M/IucMRyeWlRE4pCtry68ENpojXr0tdyyVs8XSkgCGgup/BqDTkBnEBD+V5hOIrHJv5rJ6KpaxEZG0ozUJda" +
      "UpCseSSKK4Jn7liqVqF5EzOOXelqTAACcJmILKQHqke8n3imNs72oi8tu1N+oqbFp60K9whtLDm0JZSavpmRDkMODb8/4FOusBHFYZC" +
      "uxMUmotN9Dkzp4InT7kJdKZ/kr61SMhU4hj7vTdjhcRHItO2P+jR7+38kQLDR4O1HR1XkHzLMwDvDwZULeOl6afS1ZpbO8XpePHaaLn" +
      "EqJeZ8BpnfwBEiylK3HGzGAP7WcAgFlMO9AEqoGnnbUBFcL+IYnZ3JFPy0sGsrH4cOC8Gxy2icQKrGpdIyMqGjb2hZsSc1S4njGpK0A" +
      "lCEKrAjzpr6SzPSwLnFtAJpztHbgb9Z7D2jdsjugQYdFwi6/9GKOI/slKqt5/vb7dLnSyeAY+jTaoveUZf6D5yM8PCKrvw5/k+A1XJw" +
      "=="
  })
  void negative(String fileContent) throws IOException {
    assertThat(analyze(check, fileContent)).isEmpty();
  }

}
