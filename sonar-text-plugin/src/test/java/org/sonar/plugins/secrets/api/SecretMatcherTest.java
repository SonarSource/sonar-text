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

import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.sonar.plugins.secrets.configuration.deserialization.ReferenceTestModel;
import org.sonar.plugins.secrets.configuration.model.Rule;

import static org.sonar.plugins.secrets.api.AuxiliaryPatternMatcherFactoryTest.constructReferenceAuxiliaryMatcher;
import static org.sonar.plugins.secrets.api.SecretMatcherAssert.assertThat;

class SecretMatcherTest {

  @Test
  void testConstructionOfSimpleDetection() {
    Rule rule = ReferenceTestModel.constructMinimumSpecification().getProvider().getRules().get(0);

    PatternMatcher patternMatcher = new PatternMatcher("\\b(rule matching pattern)\\b");
    SecretMatcher expectedMatcher = new SecretMatcher(rule, patternMatcher, AuxiliaryPatternMatcher.NO_FILTERING_AUXILIARY_MATCHER,
        file -> true, s -> true);

    SecretMatcher actualMatcher = SecretMatcher.build(rule);

    assertThat(actualMatcher).behavesLike(expectedMatcher);
  }

  @Test
  void testConstructionOfDetectionWithPostFilter() {
    Rule rule = ReferenceTestModel.constructReferenceSpecification().getProvider().getRules().get(0);

    PatternMatcher patternMatcher = new PatternMatcher("\\b(rule matching pattern)\\b");

    Predicate<String> expectedPredicate = s -> true;
    Predicate<String> patternNotFilter = candidateSecret -> {
      Matcher matcher = Pattern.compile("EXAMPLEKEY").matcher(candidateSecret);
      return !matcher.find();
    };
    Predicate<String> statisticalFilter = candidateSecret -> !EntropyChecker.hasLowEntropy(candidateSecret, 4.2f);
    expectedPredicate = expectedPredicate.and(statisticalFilter).and(patternNotFilter);
    SecretMatcher expectedMatcher = new SecretMatcher(rule, patternMatcher,
      constructReferenceAuxiliaryMatcher(),file -> true,
      expectedPredicate);

    SecretMatcher actualMatcher = SecretMatcher.build(rule);

    assertThat(actualMatcher).behavesLike(expectedMatcher);
  }

  @Test
  void testConstructionOfDetectionWithoutMatching() {
    Rule rule = ReferenceTestModel.constructMinimumSpecification().getProvider().getRules().get(0);
    rule.getDetection().setMatching(null);
    PatternMatcher patternMatcher = new PatternMatcher(null);
    SecretMatcher expectedMatcher = new SecretMatcher(rule, patternMatcher, AuxiliaryPatternMatcher.NO_FILTERING_AUXILIARY_MATCHER, file -> true,
      s -> true);

    SecretMatcher actualMatcher = SecretMatcher.build(rule);

    assertThat(actualMatcher).behavesLike(expectedMatcher);
  }

}
