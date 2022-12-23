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

public class AwsSessionTokenRuleTest {

  AwsSessionTokenRule underTest = new AwsSessionTokenRule();

  @Test
  public void testRuleProperties() {
    assertThat(underTest.getRuleKey()).isEqualTo("S6290");
  }

  @Test
  public void testRuleRegexPositive() {
    List<Secret> secrets = underTest.findSecretsIn(TestUtils.aNormalizedInputFile(
      "AWS_SESSION_TOKEN=IQoJb3JpZ2luX2VjEKL//////////wEaDGV1LWNlbnRyYWwtMSJHMEUCIQDFlDUEvUa6slxlkKKn8zbLkN/j1f7lKJdXJ03PQ5T5ZwIgDYlshciO8nyfnmjUfFy4I2+rEuPHBexsvfBo3MlCdgQqugMISxAAGgw4NTk4OTY2NzUzMDYiDFKPV7D/QmnqFWRYpiqXAypJf6TksPZXImVpIUU0Yj0uJhNN0o/HcO8hfQ4BXuCvpm1DOiVsH6VXMxgNdpGTWr8CjNpEt/eYwSk6MAVPOtjg5+lY2qoGJrUuxwhiKe+BquVM17h0giZ18h1B4ozDGkfxA/vGSJa/qBznF0yEpLE+fJoesGe4ZpATs8oUN94/XkrL/eYzXsW3ZD1ZX66QzmSFHhgTJc24d9bezGjR32fEJD/dBm9La+7wpc4+jrXCmt6yxHox0gCuGrSagcJfPh9pVYneM81fnD/S7Kicb1Pw8MiChfqW0hao1twr4wMgp9N3JlYQNK3fZKbMU/qlvoKTz8D0Joa4elSp4rU4reVUsujCXVE95PDyj4LD3IDXHF5SAd/23/M/IucMRyeWlRE4pCtry68ENpojXr0tdyyVs8XSkgCGgup/BqDTkBnEBD+V5hOIrHJv5rJ6KpaxEZG0ozUJdaUpCseSSKK4Jn7liqVqF5EzOOXelqTAACcJmILKQHqke8n3imNs72oi8tu1N+oqbFp60K9whtLDm0JZSavpmRDkMODb8/4FOusBHFYZCuxMUmotN9Dkzp4InT7kJdKZ/kr61SMhU4hj7vTdjhcRHItO2P+jR7+38kQLDR4O1HR1XkHzLMwDvDwZULeOl6afS1ZpbO8XpePHaaLnEqJeZ8BpnfwBEiylK3HGzGAP7WcAgFlMO9AEqoGnnbUBFcL+IYnZ3JFPy0sGsrH4cOC8Gxy2icQKrGpdIyMqGjb2hZsSc1S4njGpK0AlCEKrAjzpr6SzPSwLnFtAJpztHbgb9Z7D2jdsjugQYdFwi6/9GKOI/slKqt5/vb7dLnSyeAY+jTaoveUZf6D5yM8PCKrvw5/k+A1XJw=="));

    assertThat(secrets)
      .extracting(Secret::getTextRange)
      .extracting("start.line", "start.lineOffset", "end.line", "end.lineOffset")
      .containsOnly(tuple(1, 17, 1, 1086));
  }

  @Test
  public void testRuleRegexNegative() {
    List<Secret> secrets = underTest.findSecretsIn(TestUtils.aNormalizedInputFile(
      "AWS_SESS_TOK=IQoJb3JpZ2luX2VjEKL//////////wEaDGV1LWNlbnRyYWwtMSJHMEUCIQDFlDUEvUa6slxlkKKn8zbLkN/j1f7lKJdXJ03PQ5T5ZwIgDYlshciO8nyfnmjUfFy4I2+rEuPHBexsvfBo3MlCdgQqugMISxAAGgw4NTk4OTY2NzUzMDYiDFKPV7D/QmnqFWRYpiqXAypJf6TksPZXImVpIUU0Yj0uJhNN0o/HcO8hfQ4BXuCvpm1DOiVsH6VXMxgNdpGTWr8CjNpEt/eYwSk6MAVPOtjg5+lY2qoGJrUuxwhiKe+BquVM17h0giZ18h1B4ozDGkfxA/vGSJa/qBznF0yEpLE+fJoesGe4ZpATs8oUN94/XkrL/eYzXsW3ZD1ZX66QzmSFHhgTJc24d9bezGjR32fEJD/dBm9La+7wpc4+jrXCmt6yxHox0gCuGrSagcJfPh9pVYneM81fnD/S7Kicb1Pw8MiChfqW0hao1twr4wMgp9N3JlYQNK3fZKbMU/qlvoKTz8D0Joa4elSp4rU4reVUsujCXVE95PDyj4LD3IDXHF5SAd/23/M/IucMRyeWlRE4pCtry68ENpojXr0tdyyVs8XSkgCGgup/BqDTkBnEBD+V5hOIrHJv5rJ6KpaxEZG0ozUJdaUpCseSSKK4Jn7liqVqF5EzOOXelqTAACcJmILKQHqke8n3imNs72oi8tu1N+oqbFp60K9whtLDm0JZSavpmRDkMODb8/4FOusBHFYZCuxMUmotN9Dkzp4InT7kJdKZ/kr61SMhU4hj7vTdjhcRHItO2P+jR7+38kQLDR4O1HR1XkHzLMwDvDwZULeOl6afS1ZpbO8XpePHaaLnEqJeZ8BpnfwBEiylK3HGzGAP7WcAgFlMO9AEqoGnnbUBFcL+IYnZ3JFPy0sGsrH4cOC8Gxy2icQKrGpdIyMqGjb2hZsSc1S4njGpK0AlCEKrAjzpr6SzPSwLnFtAJpztHbgb9Z7D2jdsjugQYdFwi6/9GKOI/slKqt5/vb7dLnSyeAY+jTaoveUZf6D5yM8PCKrvw5/k+A1XJw=="));

    assertThat(secrets).isEmpty();
  }
}
