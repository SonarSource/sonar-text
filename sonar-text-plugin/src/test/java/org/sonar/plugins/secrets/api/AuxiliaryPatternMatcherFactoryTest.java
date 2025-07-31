/*
 * SonarQube Text Plugin
 * Copyright (C) 2021-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
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
import static org.sonar.plugins.common.TestUtils.mockDurationStatistics;

class AuxiliaryPatternMatcherFactoryTest {

  @Test
  void testConstructionWhenNoContextIsGivenInRule() {
    Rule rule = ReferenceTestModel.constructMinimumSpecification().getProvider().getRules().get(0);

    var configuration = SpecificationConfiguration.AUTO_TEST_FILE_DETECTION_ENABLED;
    AuxiliaryPatternMatcher auxiliaryPatternMatcher = SecretMatcher.build(rule, mockDurationStatistics(), configuration, true)
      .getAuxiliaryPatternMatcher();

    assertThat(auxiliaryPatternMatcher).isEqualTo(AuxiliaryPatternMatcher.NO_FILTERING_AUXILIARY_MATCHER);
  }

  @Test
  void testConstructionOfReferenceAuxiliaryMatcher() {
    Rule rule = ReferenceTestModel.constructReferenceSpecification().getProvider().getRules().get(0);

    AuxiliaryPatternMatcher expected = constructReferenceAuxiliaryMatcher();
    AuxiliaryPatternMatcher matcher = AuxiliaryPatternMatcherFactory.build(rule.getDetection().getMatching());

    BiPredicate<Pattern, Pattern> patternEquals = (p1, p2) -> Objects.equals(p1.pattern(), p2.pattern());

    assertThat(matcher).usingRecursiveComparison().withEqualsForType(patternEquals, Pattern.class).isEqualTo(expected);
  }

  public static AuxiliaryPatternMatcher constructReferenceAuxiliaryMatcher() {
    AuxiliaryPattern patternBefore = ReferenceTestModel.constructAuxiliaryPattern(AuxiliaryPatternType.PATTERN_BEFORE, "\\b(pattern" +
      "-before)\\b");
    AuxiliaryPattern patternBeforeForMatchNot = ReferenceTestModel.constructAuxiliaryPattern(AuxiliaryPatternType.PATTERN_BEFORE, "\\b(match-not-before)\\b");
    patternBeforeForMatchNot.setMaxCharacterDistance(100);
    patternBeforeForMatchNot.setMaxLineDistance(50);
    AuxiliaryPattern patternAfter = ReferenceTestModel.constructAuxiliaryPattern(AuxiliaryPatternType.PATTERN_AFTER, "\\b(pattern-after)" +
      "\\b");
    AuxiliaryPattern patternAround = ReferenceTestModel.constructAuxiliaryPattern(AuxiliaryPatternType.PATTERN_AROUND, "\\b(pattern" +
      "-around)\\b");
    AuxiliaryPattern patternAroundCharDistance = ReferenceTestModel.constructAuxiliaryPattern(AuxiliaryPatternType.PATTERN_AROUND, "\\b(pattern" +
      "-maxCharDistance-around)\\b");
    patternAroundCharDistance.setMaxCharacterDistance(100);
    AuxiliaryPattern patternAroundLineDistance = ReferenceTestModel.constructAuxiliaryPattern(AuxiliaryPatternType.PATTERN_AROUND, "\\b(pattern-maxLineDistance-around)\\b");
    patternAroundLineDistance.setMaxLineDistance(5);

    AuxiliaryPatternMatcher matcherBefore = AuxiliaryMatcher.build(patternBefore);
    AuxiliaryPatternMatcher matcherNegation = AuxiliaryMatcher.build(patternBeforeForMatchNot);
    AuxiliaryPatternMatcher matcherAfter = AuxiliaryMatcher.build(patternAfter);
    AuxiliaryPatternMatcher matcherAround = AuxiliaryMatcher.build(patternAround);
    AuxiliaryPatternMatcher matcherAroundCharDistance = AuxiliaryMatcher.build(patternAroundCharDistance);
    AuxiliaryPatternMatcher matcherAroundLineDistance = AuxiliaryMatcher.build(patternAroundLineDistance);

    AuxiliaryPatternMatcher eachSecondLevel = matcherAfter.and(matcherAround);
    AuxiliaryPatternMatcher eitherSecondLevel = matcherAroundLineDistance.or(matcherAroundCharDistance);
    return matcherBefore.or(matcherNegation.negate()).or(eachSecondLevel).or(eitherSecondLevel);
  }
}
