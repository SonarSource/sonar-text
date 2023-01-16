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

import org.sonar.check.Rule;
import org.sonar.plugins.secrets.api.EntropyChecker;
import org.sonar.plugins.secrets.api.RegexMatcher;
import org.sonar.plugins.secrets.api.SecretCheck;
import org.sonar.plugins.secrets.api.SecretRule;

@Rule(key = "S6336")
public class AlibabaCloudAccessKeyCheck extends SecretCheck {
  public AlibabaCloudAccessKeyCheck() {
    super(
      // Alibaba Cloud Access Key ID
      new SecretRule(
        "Make sure this Alibaba Cloud Access Key ID is not disclosed.",
        new RegexMatcher("\\b(LTAI[0-9A-Za-z]{12}(:?[0-9A-Za-z]{8})?)\\b")),
      // Alibaba Cloud Access Key Secret
      new SecretRule(
        "Make sure this Alibaba Cloud Access Key Secret is not disclosed.",
        EntropyChecker::hasLowEntropy,
        new RegexMatcher("(?i)(?<![A-Z])ali(?:yun|baba|cloud).{0,50}['\"`]([0-9a-z]{30})['\"`]"),
        new RegexMatcher("(?i)(?:SECRET_?(?:ACCESS)?_?KEY|(?:ACCESS)?_?KEY_?SECRET)\\b[^0-9a-z]{0,10}([0-9a-z]{30})(?![a-z0-9\\/+=$\\-_])")));
  }
}
