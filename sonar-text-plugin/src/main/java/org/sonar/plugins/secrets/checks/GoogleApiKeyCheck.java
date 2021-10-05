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
package org.sonar.plugins.secrets.checks;

import org.sonar.check.Rule;
import org.sonar.plugins.secrets.api.RegexMatcher;
import org.sonar.plugins.secrets.api.SecretCheck;
import org.sonar.plugins.secrets.api.SecretRule;

@Rule(key = "S6334")
public class GoogleApiKeyCheck extends SecretCheck {
  public GoogleApiKeyCheck() {
    super(new SecretRule(
      "Make sure this Google API Key is not disclosed.",
      new RegexMatcher("(AIza[0-9A-Za-z\\-_]{35})\\b")));
  }
}