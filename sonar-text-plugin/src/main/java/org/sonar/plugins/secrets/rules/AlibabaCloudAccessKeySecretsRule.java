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

import org.sonar.plugins.secrets.EntropyChecker;
import org.sonar.plugins.secrets.rules.matching.RegexMatcher;

public class AlibabaCloudAccessKeySecretsRule extends AbstractAlibabaCloudAccessKeyRule {

  public AlibabaCloudAccessKeySecretsRule() {
    super("Make sure this Alibaba Cloud Access Key Secret is not disclosed.",
      new RegexMatcher("(?i)ali(?:yun|baba|cloud).{0,50}['\"`]([0-9a-z]{30})['\"`]"),
      new RegexMatcher("(?i)(?:SECRET_?(?:ACCESS)?_?KEY|(?:ACCESS)?_?KEY_?SECRET)\\b[^0-9a-z]{0,10}([0-9a-z]{30})[^a-z0-9\\/+=$\\-_]"));
  }

  @Override
  public boolean isProbablyFalsePositive(String matchedText) {
    return EntropyChecker.hasLowEntropy(matchedText);
  }

}
