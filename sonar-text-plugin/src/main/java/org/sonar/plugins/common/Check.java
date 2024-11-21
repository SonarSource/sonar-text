/*
 * SonarQube Text Plugin
 * Copyright (C) 2021-2024 SonarSource SA
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

import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.AnnotationUtils;
import org.sonar.check.Rule;

public abstract class Check {

  private RuleKey ruleKey;

  protected Check() {
    Rule ruleAnnotation = AnnotationUtils.getAnnotation(getClass(), Rule.class);
    if (ruleAnnotation == null) {
      throw new IllegalStateException("@Rule annotation was not found on " + getClass().getName());
    }
    String ruleId = ruleAnnotation.key();
    if (ruleId.isEmpty()) {
      throw new IllegalStateException("Empty @Rule key on " + getClass().getName());
    }
    ruleKey = RuleKey.of(repositoryKey(), ruleId);
  }

  protected abstract String repositoryKey();

  public abstract void analyze(InputFileContext ctx);

  public RuleKey getRuleKey() {
    return ruleKey;
  }

  public void setRuleKey(RuleKey ruleKey) {
    this.ruleKey = ruleKey;
  }

}
