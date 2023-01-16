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

@Rule(key = "S6290")
public class AwsCheck extends SecretCheck {
  private static final String NO_MATCH_SUFFIX = "EXAMPLE";

  public AwsCheck() {
    super(
      // Aws Access Key
      new SecretRule(
        "Make sure this AWS Secret Access Key is not disclosed.",
        EntropyChecker::hasLowEntropy,
        new RegexMatcher("(?i)aws.{0,50}['\"`]([0-9a-z\\/+]{40})['\"`]"),
        new RegexMatcher("(?i)\\b(?:AWS)?_?SECRET_?(?:ACCESS)?_?KEY\\b.{0,10}\\b([0-9a-z\\/+]{40})\\b")),
      // Aws Access Key ID
      new SecretRule(
        "Make sure this AWS Access Key ID is not disclosed.",
        (String matchedText) -> matchedText.endsWith(NO_MATCH_SUFFIX),
        new RegexMatcher("\\b((?:AKIA|ASIA)[A-Z0-9]{16})\\b")),
      // Aws Session Token
      new SecretRule(
        "Make sure this AWS Session Token is not disclosed.",
        new RegexMatcher("(?i)session_?token.*?([0-9a-z\\/+=]{100,})")));
  }
}
