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

import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.sonar.plugins.secrets.configuration.deserialization.ReferenceTestModel;
import org.sonar.plugins.secrets.configuration.model.Rule;

import static org.assertj.core.api.Assertions.assertThat;

class SecretsMatcherFactoryTest {

  @Test
  void testDetectionWithSimplePattern() {
    Rule rule = ReferenceTestModel.constructRule();
    RegexMatcher expectedMatcher = new RegexMatcher("\\b(rule matching pattern)\\b");

    SecretsMatcher secretsMatcher = SecretsMatcherFactory.constructSecretsMatcher(rule);

    BiPredicate<Pattern, Pattern> patternEquals = (p1, p2) -> Objects.equals(p1.pattern(), p2.pattern());
    assertThat(secretsMatcher)
      .usingRecursiveComparison()
      .withEqualsForType(patternEquals, Pattern.class)
      .isEqualTo(expectedMatcher);
  }

  @Test
  void produceNoDetectionMatcherWhenMatchingIsNull() {
    Rule rule = ReferenceTestModel.constructRule();
    rule.getDetection().setMatching(null);

    SecretsMatcher secretsMatcher = SecretsMatcherFactory.constructSecretsMatcher(rule);

    assertThat(secretsMatcher).isEqualTo(SecretsMatcherFactory.NO_DETECTION_MATCHER);
  }
}
