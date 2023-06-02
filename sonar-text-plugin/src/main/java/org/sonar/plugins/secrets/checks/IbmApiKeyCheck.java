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

import java.util.function.Predicate;
import org.sonar.check.Rule;
import org.sonar.plugins.secrets.api.EntropyChecker;
import org.sonar.plugins.secrets.api.RegexMatcher;
import org.sonar.plugins.secrets.api.SecretCheck;
import org.sonar.plugins.secrets.api.SecretRule;

@Rule(key = "S6337")
public class IbmApiKeyCheck extends SecretCheck {
  public IbmApiKeyCheck() {
    super(new SecretRule(
      "Make sure this IBM API key is not disclosed.",
      ((Predicate<String>) EntropyChecker::hasLowEntropy),
      new RegexMatcher("(?is)(?:ibm|apikey).{0,50}['\"`]([a-z0-9_\\-]{44})['\"`]")));
  }
}
