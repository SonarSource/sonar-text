/*
 * SonarQube Text Plugin
 * Copyright (C) 2021-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.plugins.secrets.checks;

import java.io.IOException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.sonar.plugins.common.Check;
import org.sonar.plugins.secrets.utils.AbstractRuleExampleTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.plugins.common.TestUtils.analyze;

@SuppressWarnings("squid:S6290")
class AwsCheckTest extends AbstractRuleExampleTest {

  protected AwsCheckTest() {
    super(new AwsCheck());
  }

  @ParameterizedTest
  @ValueSource(strings = {
    // key id with EXAMPLE
    """
      public class Foo {
        public static final String KEY = "AKIAIGKECZXA7EXAMPLE"
      }""",
    // not an access key
    """
      public class Foo {
        public static final String KEY = "AKIGKECZXA7AEIJLMQ"
      }"""
  })
  void negative(String fileContent) throws IOException {
    Check check = getInitializedCheck();
    assertThat(analyze(check, fileContent)).isEmpty();
  }
}
