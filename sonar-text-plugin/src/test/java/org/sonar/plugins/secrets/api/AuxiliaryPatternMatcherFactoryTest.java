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
import org.sonar.plugins.secrets.configuration.model.matching.AuxiliaryPattern;
import org.sonar.plugins.secrets.configuration.model.matching.AuxiliaryPatternType;

import static org.assertj.core.api.Assertions.assertThat;

class AuxiliaryPatternMatcherFactoryTest {

  @Test
  void testConstructionWhenNoContextIsGivenInRule() {
    Rule rule = ReferenceTestModel.constructMinimumSpecification().getProvider().getRules().get(0);

    AuxiliaryPatternMatcher auxiliaryPatternMatcher = SecretMatcher.build(rule).getAuxiliaryPatternMatcher();

    assertThat(auxiliaryPatternMatcher).isEqualTo(AuxiliaryPatternMatcher.NO_FILTERING_AUXILIARY_MATCHER);
  }

  @Test
  void testConstructionOfReferenceAuxiliaryMatcher() {
    Rule rule = ReferenceTestModel.constructReferenceSpecification().getProvider().getRules().get(0);

    AuxiliaryPatternMatcher expected = constructReferenceAuxiliaryMatcher();
    AuxiliaryPatternMatcher matcher = AuxiliaryPatternMatcherFactory.build(rule.getDetection().getMatching());

    BiPredicate<Pattern, Pattern> patternEquals = (p1, p2) -> Objects.equals(p1.pattern(), p2.pattern());

    assertThat(matcher).usingRecursiveComparison().
      withEqualsForType(patternEquals, Pattern.class).isEqualTo(expected);
  }

  public static AuxiliaryPatternMatcher constructReferenceAuxiliaryMatcher() {
    AuxiliaryPattern patternBefore = ReferenceTestModel.constructAuxiliaryPattern(AuxiliaryPatternType.PATTERN_BEFORE, "\\b(pattern" +
      "-before)\\b");
    AuxiliaryPattern patternAfter = ReferenceTestModel.constructAuxiliaryPattern(AuxiliaryPatternType.PATTERN_AFTER, "\\b(pattern-after)" +
      "\\b");
    AuxiliaryPattern patternAround = ReferenceTestModel.constructAuxiliaryPattern(AuxiliaryPatternType.PATTERN_AROUND, "\\b(pattern" +
      "-around)\\b");
    AuxiliaryPattern patternNot = ReferenceTestModel.constructAuxiliaryPattern(AuxiliaryPatternType.PATTERN_NOT, "\\b(pattern-not)\\b");
    AuxiliaryPattern patternAroundLevel2 = ReferenceTestModel.constructAuxiliaryPattern(AuxiliaryPatternType.PATTERN_AROUND, "\\b(pattern" +
      "-around)\\b");

    AuxiliaryPatternMatcher matcherBefore = AuxiliaryMatcher.build(patternBefore);
    AuxiliaryPatternMatcher matcherAfter = AuxiliaryMatcher.build(patternAfter);
    AuxiliaryPatternMatcher matcherAround = AuxiliaryMatcher.build(patternAround);
    AuxiliaryPatternMatcher matcherNot = AuxiliaryMatcher.build(patternNot);
    AuxiliaryPatternMatcher matcherAroundLevel2 = AuxiliaryMatcher.build(patternAroundLevel2);

    AuxiliaryPatternMatcher eachSecondLevel = matcherAfter.and(matcherAround);
    AuxiliaryPatternMatcher eitherSecondLevel = matcherNot.or(matcherAroundLevel2);
    return matcherBefore.or(eachSecondLevel).or(eitherSecondLevel);
  }
}
