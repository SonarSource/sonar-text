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
package org.sonar.plugins.secrets.api;

import java.util.Collections;
import java.util.List;
import org.sonar.plugins.secrets.configuration.model.Rule;
import org.sonar.plugins.secrets.configuration.model.matching.PatternMatch;
import org.sonar.plugins.secrets.configuration.model.matching.PatternType;

public class SecretsMatcherFactory {

  private static final SecretsMatcher NO_DETECTION_MATCHER = s -> Collections.emptyList();

  private SecretsMatcherFactory() {
  }

  public static List<SecretsMatcher> constructSecretMatchers(Rule rule) {
    if (rule.getDetection().getMatching() instanceof PatternMatch) {
      return List.of(constructSecretMatchers((PatternMatch) rule.getDetection().getMatching()));
    } else {
      return List.of(NO_DETECTION_MATCHER);
    }
  }

  private static SecretsMatcher constructSecretMatchers(PatternMatch match) {
    if (PatternType.PATTERN == match.getType()) {
      return new RegexMatcher(match.getPattern());
    } else {
      return NO_DETECTION_MATCHER;
    }
  }

}
