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
package org.sonar.plugins.common;

import org.junit.jupiter.api.Test;
import org.sonar.check.Rule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CheckTest {

  @Test
  void rule_id() {
    Check validRuleKeyCheck = new ValidRuleKeyCheck();
    assertThat(validRuleKeyCheck.getRuleKey().repository()).isEqualTo("test");
    assertThat(validRuleKeyCheck.getRuleKey().rule()).isEqualTo("bar");

    assertThatThrownBy(EmptyCheck::new)
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("@Rule annotation was not found on org.sonar.plugins.common.CheckTest$EmptyCheck");

    assertThatThrownBy(InvalidRuleKeyCheck::new)
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Empty @Rule key on org.sonar.plugins.common.CheckTest$InvalidRuleKeyCheck");
  }

  static class EmptyCheck extends Check {
    @Override
    protected String repositoryKey() {
      return "test";
    }

    @Override
    public void analyze(InputFileContext ctx) {
      // empty
    }
  }

  @Rule(name = "foo")
  static class InvalidRuleKeyCheck extends EmptyCheck {
  }

  @Rule(key = "bar")
  static class ValidRuleKeyCheck extends EmptyCheck {
  }

}
