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
package org.sonar.plugins.common;

import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.AnnotationUtils;
import org.sonar.check.Rule;

public interface Check {

  void analyze(InputFileContext ctx);

  RuleKey ruleKey();

  default String ruleId() {
    Rule ruleAnnotation = AnnotationUtils.getAnnotation(getClass(), Rule.class);
    if (ruleAnnotation == null) {
      throw new IllegalStateException("No Rule annotation was found on " + getClass().getName());
    }
    String ruleKey = ruleAnnotation.key();
    if (ruleKey.length() == 0) {
      throw new IllegalStateException("Empty key");
    }
    return ruleKey;
  }

}
