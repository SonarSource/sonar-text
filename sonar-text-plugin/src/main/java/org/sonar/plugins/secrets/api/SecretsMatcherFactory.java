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
import org.sonar.plugins.secrets.configuration.model.Rule;
import org.sonar.plugins.secrets.configuration.model.matching.AuxiliaryPattern;
import org.sonar.plugins.secrets.configuration.model.matching.AuxiliaryPatternType;

public class SecretsMatcherFactory {

  static final SecretsMatcher NO_DETECTION_MATCHER = s -> Collections.emptyList();

  private SecretsMatcherFactory() {
  }

  public static SecretsMatcher constructSecretsMatcher(Rule rule) {
    if (rule.getDetection().getMatching() instanceof AuxiliaryPattern) {
      return constructSecretsMatcher((AuxiliaryPattern) rule.getDetection().getMatching());
    } else {
      return NO_DETECTION_MATCHER;
    }
  }

  static SecretsMatcher constructSecretsMatcher(AuxiliaryPattern match) {
    if (AuxiliaryPatternType.PATTERN == match.getType()) {
      return new RegexMatcher(match.getPattern());
    } else {
      return NO_DETECTION_MATCHER;
    }
  }

}
