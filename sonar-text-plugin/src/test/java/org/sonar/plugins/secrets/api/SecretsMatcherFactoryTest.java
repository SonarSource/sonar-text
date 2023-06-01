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

import org.junit.jupiter.api.Test;
import org.sonar.plugins.secrets.configuration.deserialization.ReferenceTestModel;
import org.sonar.plugins.secrets.configuration.model.Rule;
import org.sonar.plugins.secrets.configuration.model.matching.AuxiliaryPattern;
import org.sonar.plugins.secrets.configuration.model.matching.AuxiliaryPatternType;

import static org.assertj.core.api.Assertions.assertThat;

class SecretsMatcherFactoryTest {

  @Test
  void detectionWithBooleanMatchProducesNoDetectionMatcher() {
    Rule rule = ReferenceTestModel.constructRule();
    ReferenceTestModel.enrichDetection(rule.getDetection());

    SecretsMatcher secretsMatcher = SecretsMatcherFactory.constructSecretsMatcher(rule);

    assertThat(secretsMatcher).isEqualTo(SecretsMatcherFactory.NO_DETECTION_MATCHER);
  }

  @Test
  void detectionWithAuxiliaryPatternMatchProducesNoDetectionMatcher() {
    AuxiliaryPattern auxiliaryPattern = ReferenceTestModel.constructAuxiliaryPattern(AuxiliaryPatternType.PATTERN_AFTER, "pattern");

    SecretsMatcher secretsMatcher = SecretsMatcherFactory.constructSecretsMatcher(auxiliaryPattern);

    assertThat(secretsMatcher).isEqualTo(SecretsMatcherFactory.NO_DETECTION_MATCHER);
  }
}
