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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.sonar.plugins.secrets.TestUtils.aNormalizedInputFile;

public class AwsAccessKeyIdRuleTest {

  AwsAccessKeyIdRule underTest = new AwsAccessKeyIdRule();

  @Test
  public void testRuleProperties() {
    assertThat(underTest.getRuleKey()).isEqualTo("S6290");
  }

  @Test
  public void testRuleRegexPositive() {
    List<Secret> secrets = underTest.findSecretsIn(aNormalizedInputFile("public class Foo {\n"
      + "  public static final String KEY = \"AKIAIGKECZXA7AEIJLMQ\"\n"
      + "}"));

    assertThat(secrets)
      .extracting(Secret::getTextRange)
      .extracting("start.line", "start.lineOffset", "end.line", "end.lineOffset")
      .containsOnly(tuple(2, 36, 2, 56));
  }

  @Test
  public void testRuleRegexNegative() {
    List<Secret> secrets = underTest.findSecretsIn(aNormalizedInputFile("public class Foo {\n"
      + "  public static final String KEY = \"AKIGIGKECZXA7AEIJLMQ\"\n"
      + "}"));

    assertThat(secrets).isEmpty();
  }

  @Test
  public void testRuleRegexExamplePositive() {
    List<Secret> secrets = underTest.findSecretsIn(aNormalizedInputFile("public class Foo {\n"
      + "  public static final String KEY = \"AKIAIGKECZXA7EXAMPLF\"\n"
      + "}"));

    assertThat(secrets)
      .extracting(Secret::getTextRange)
      .extracting("start.line", "start.lineOffset", "end.line", "end.lineOffset")
      .containsOnly(tuple(2, 36, 2, 56));
  }

  @Test
  public void testRuleRegexExampleNegative() {
    List<Secret> secrets = underTest.findSecretsIn(aNormalizedInputFile("public class Foo {\n"
      + "  public static final String KEY = \"AKIAIGKECZXA7EXAMPLE\"\n"
      + "}"));

    assertThat(secrets).isEmpty();
  }

}
