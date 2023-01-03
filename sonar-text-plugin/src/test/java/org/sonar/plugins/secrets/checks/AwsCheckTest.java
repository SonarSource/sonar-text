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

class AwsCheckTest {

  Check check = new AwsCheck();

  @Test
  void testAwsAccessKeyIdPositive() throws IOException {
    Collection<Issue> issues = analyze(check, inputFile("public class Foo {\n"
      + "  public static final String KEY = \"AKIAIGKECZXA7AEIJLMQ\"\n"
      + "}"));
    assertThat(asString(issues)).containsExactly(
      "secrets:S6290 [2:36-2:56] Make sure this AWS Access Key ID is not disclosed.");
  }

  @Test
  void testAwsAccessKeyIdNegative() throws IOException {
    Collection<Issue> issues = analyze(check, inputFile("public class Foo {\n"
      + "  public static final String KEY = \"AKIGIGKECZXA7AEIJLMQ\"\n"
      + "}"));
    assertThat(issues).isEmpty();
  }

  @Test
  void testAwsAccessKeyIdExamplePositive() throws IOException {
    Collection<Issue> issues = analyze(check, inputFile("public class Foo {\n"
      + "  public static final String KEY = \"AKIAIGKECZXA7EXAMPLF\"\n"
      + "}"));
    assertThat(asString(issues)).containsExactly(
      "secrets:S6290 [2:36-2:56] Make sure this AWS Access Key ID is not disclosed.");
  }

  @Test
  void testAwsAccessKeyIdExampleNegative() throws IOException {
    Collection<Issue> issues = analyze(check, inputFile("public class Foo {\n"
      + "  public static final String KEY = \"AKIAIGKECZXA7EXAMPLE\"\n"
      + "}"));
    assertThat(issues).isEmpty();
  }

  @Test
  void testAwsAccessKeyFirstPositive() throws IOException {
    Collection<Issue> issues = analyze(check, inputFile("var creds = new AWS.Credentials({ " +
      "     secretAccessKey: 'kHeUAwnSUizTWpSbyGAz4f+As5LshPIjvtpswqGb' " +
      "});"));
    assertThat(asString(issues)).containsExactly(
      "secrets:S6290 [1:57-1:97] Make sure this AWS Secret Access Key is not disclosed.");
  }

  @Test
  void testAwsAccessKeySecondPositive() throws IOException {
    Collection<Issue> issues = analyze(check, inputFile("aws_secret_access_key=kHeUAwnSUizTWpSbyGAz4f+As5LshPIjvtpswqGb"));
    assertThat(asString(issues)).containsExactly(
      "secrets:S6290 [1:22-1:62] Make sure this AWS Secret Access Key is not disclosed.");
  }

  @Test
  void testAwsAccessKeyNegative() throws IOException {
    Collection<Issue> issues = analyze(check, inputFile("public class Foo {\n"
      + "  public static final String KEY = \"AKIGKECZXA7AEIJLMQ\"\n"
      + "}"));
    assertThat(issues).isEmpty();
  }

  @Test
  void testAwsSessionTokenPositive() throws IOException {
    Collection<Issue> issues = analyze(check, inputFile("AWS_SESSION_TOKEN=IQoJb3JpZ2luX2VjEKL//////////wE" +
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
      "A1XJw=="));
    assertThat(asString(issues)).containsExactly(
      "secrets:S6290 [1:17-1:1086] Make sure this AWS Session Token is not disclosed.");
  }

  @Test
  void testAwsSessionTokenNegative() throws IOException {
    Collection<Issue> issues = analyze(check, inputFile("AWS_SESS_TOK=IQoJb3JpZ2luX2VjEKL//////////wEaDGV1" +
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
      "=="));
    assertThat(issues).isEmpty();
  }

}
