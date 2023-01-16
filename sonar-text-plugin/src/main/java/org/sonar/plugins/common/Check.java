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
package org.sonar.plugins.common;

import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.AnnotationUtils;
import org.sonar.check.Rule;

public abstract class Check {

  public final RuleKey ruleKey;

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

}
