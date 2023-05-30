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
package org.sonar.plugins.secrets.configuration.deserialization;

import java.util.ArrayList;
import java.util.List;
import org.sonar.plugins.secrets.configuration.model.Provider;
import org.sonar.plugins.secrets.configuration.model.Rule;
import org.sonar.plugins.secrets.configuration.model.RuleExample;
import org.sonar.plugins.secrets.configuration.model.Specification;
import org.sonar.plugins.secrets.configuration.model.matching.BooleanMatch;
import org.sonar.plugins.secrets.configuration.model.matching.Detection;
import org.sonar.plugins.secrets.configuration.model.matching.Match;
import org.sonar.plugins.secrets.configuration.model.matching.MatchingType;
import org.sonar.plugins.secrets.configuration.model.matching.PatternMatch;
import org.sonar.plugins.secrets.configuration.model.matching.PatternType;
import org.sonar.plugins.secrets.configuration.model.matching.filter.HeuristicsFilter;
import org.sonar.plugins.secrets.configuration.model.matching.filter.IncludedFilter;
import org.sonar.plugins.secrets.configuration.model.matching.filter.PostModule;
import org.sonar.plugins.secrets.configuration.model.matching.filter.PreModule;
import org.sonar.plugins.secrets.configuration.model.matching.filter.RejectFilter;
import org.sonar.plugins.secrets.configuration.model.matching.filter.StatisticalFilter;
import org.sonar.plugins.secrets.configuration.model.metadata.ProviderMetadata;
import org.sonar.plugins.secrets.configuration.model.metadata.Reference;
import org.sonar.plugins.secrets.configuration.model.metadata.ReferenceType;
import org.sonar.plugins.secrets.configuration.model.metadata.RuleMetadata;

public class ReferenceTestModel {

  // -------------------------------------------------------
  // Methods to construct a minimum specification test model
  // -------------------------------------------------------

  public static Specification constructMinimumSpecification() {
    Specification specification = new Specification();
    specification.setProvider(constructProvider());
    return specification;
  }

  private static Provider constructProvider() {
    Provider provider = new Provider();
    provider.setRules(List.of(constructRule()));
    provider.setMetadata(constructProviderMetadata());
    return provider;
  }

  private static ProviderMetadata constructProviderMetadata() {
    ProviderMetadata providerMetadata = new ProviderMetadata();
    providerMetadata.setCategory("Cloud provider");
    providerMetadata.setMessage("provider message");
    providerMetadata.setName("provider name");
    return providerMetadata;
  }

  public static Rule constructRule() {
    Rule rule = new Rule();
    rule.setId("exampleKey");
    rule.setMetadata(constructRuleMetadata());
    rule.setDetection(constructRuleDetection());
    rule.setExamples(List.of(constructRuleExample()));
    return rule;
  }

  private static RuleMetadata constructRuleMetadata() {
    RuleMetadata ruleMetadata = new RuleMetadata();
    ruleMetadata.setName("rule name");
    ruleMetadata.setCharset("[0-9a-z\\/+]");
    return ruleMetadata;
  }

  private static Detection constructRuleDetection() {
    Detection detection = new Detection();

    PatternMatch pattern = constructPatternMatch(PatternType.PATTERN, "\\b(test pattern)\\b");
    detection.setMatching(pattern);

    return detection;
  }

  public static PatternMatch constructPatternMatch(PatternType type, String pattern) {
    PatternMatch patternMatch = new PatternMatch();
    patternMatch.setType(type);
    patternMatch.setPattern(pattern);
    return patternMatch;
  }

  private static RuleExample constructRuleExample() {
    RuleExample example = new RuleExample();
    example.setContainsSecret(true);
    example.setText("example text\n");
    return example;
  }

  // --------------------------------------------------------------------------------------
  // Methods to construct the reference specification model based on the minimum test model
  // --------------------------------------------------------------------------------------

  public static Specification constructReferenceSpecification() {
    Specification specification = constructMinimumSpecification();

    enrichMetadata(specification.getProvider().getMetadata());
    enrichRule(specification.getProvider().getRules().get(0));
    specification.getProvider().setDetection(constructProviderDetection());

    return specification;
  }

  private static void enrichRule(Rule rule) {
    enrichRuleMetadata(rule.getMetadata());
    enrichRuleExample(rule.getExamples().get(0));
    enrichRuleDetection(rule.getDetection());
  }

  private static Detection constructProviderDetection() {
    Detection detection = new Detection();
    detection.setMatching(constructPatternMatch(PatternType.PATTERN, "provider matching pattern"));

    return detection;
  }

  private static void enrichMetadata(ProviderMetadata providerMetadata) {
    providerMetadata.setReferences(constructReferenceList());
    providerMetadata.setFix("provider fix");
    providerMetadata.setImpact("provider impact");
  }

  private static List<Reference> constructReferenceList() {
    return List.of(
      constructReference("Reference 1", ReferenceType.STANDARDS),
      constructReference("Reference 2", ReferenceType.DOCUMENTATION),
      constructReference("Reference 3", ReferenceType.CONFERENCE_PRESENTATIONS),
      constructReference("Reference 4", ReferenceType.ARTICLES_AND_BLOG_POSTS)
    );
  }

  private static Reference constructReference(String description, ReferenceType type) {
    Reference reference = new Reference();
    reference.setDescription(description);
    reference.setLink("https://docs.aws.amazon.com/IAM/...");
    reference.setType(type);
    return reference;
  }

  public static void enrichRuleDetection(Detection detection) {
    detection.setPre(constructPreModule());
    detection.setPost(constructPostModule());

    BooleanMatch matchEach = new BooleanMatch();
    matchEach.setType(MatchingType.MATCH_EACH);
    matchEach.setMatches(List.of(
      constructPatternMatch(PatternType.PATTERN_AFTER, "pattern-after"),
      constructPatternMatch(PatternType.PATTERN_AROUND, "pattern-around")
    ));

    BooleanMatch matchEitherLevelTwo = new BooleanMatch();
    matchEitherLevelTwo.setType(MatchingType.MATCH_EITHER);
    matchEitherLevelTwo.setMatches(List.of(
      constructPatternMatch(PatternType.PATTERN_NOT, "pattern-not"),
      constructPatternMatch(PatternType.PATTERN_AROUND, "pattern-around")
    ));

    List<Match> matches = new ArrayList<>();
    matches.add(constructPatternMatch(PatternType.PATTERN_BEFORE, "AKIA[A-Z0-9]{16}"));
    matches.add(constructPatternMatch(PatternType.PATTERN, "[0-9a-z\\/+]{40}"));
    matches.add(matchEach);
    matches.add(matchEitherLevelTwo);

    BooleanMatch matchEither = new BooleanMatch();
    matchEither.setType(MatchingType.MATCH_EITHER);
    matchEither.setMatches(matches);
    detection.setMatching(matchEither);
  }

  private static PreModule constructPreModule() {
    PreModule preModule = new PreModule();

    IncludedFilter includedFilter = new IncludedFilter();
    includedFilter.setPaths(List.of("*.aws/config", ".env"));
    includedFilter.setExt(List.of(".config"));
    includedFilter.setContent(List.of("amazonaws.com", "aws"));

    RejectFilter rejectFilter = new RejectFilter();
    rejectFilter.setPaths(List.of("amazonaws.com", "aws"));

    preModule.setInclude(includedFilter);
    preModule.setReject(rejectFilter);

    return preModule;
  }

  private static PostModule constructPostModule() {
    PostModule postModule = new PostModule();

    StatisticalFilter statisticalFilter = new StatisticalFilter();
    statisticalFilter.setInputString("Test String");
    statisticalFilter.setThreshold(5);

    HeuristicsFilter heuristicsFilter = new HeuristicsFilter();
    heuristicsFilter.setHeuristics(List.of("exampleHeuristics"));
    heuristicsFilter.setInputString("Test String");

    postModule.setStatisticalFilter(statisticalFilter);
    postModule.setPatternNot("EXAMPLEKEY");
    postModule.setHeuristicFilter(heuristicsFilter);

    return postModule;
  }


  private static void enrichRuleMetadata(RuleMetadata ruleMetadata) {
    ruleMetadata.setDefaultProfile(false);
    ruleMetadata.setCharset("[0-9a-z\\/+]");
    ruleMetadata.setMessage("rule message");
    ruleMetadata.setImpact("rule impact");
    ruleMetadata.setFix("rule fix");
    ruleMetadata.setReferences(
      List.of(constructReference("rule reference", ReferenceType.STANDARDS)));
  }

  private static void enrichRuleExample(RuleExample example) {
    example.setMatch("LGYIh8rDziCXCgDCUbJq1h7CKwNqnpA1il4MXL+y");
  }

  // --------------------------------------------------------------------------------------
  // Methods to transform specific model elements
  // --------------------------------------------------------------------------------------
  public static void setSpecificRuleMetadataFieldsNull(RuleMetadata ruleMetadata) {
    ruleMetadata.setImpact(null);
    ruleMetadata.setFix(null);
    ruleMetadata.setMessage(null);
    ruleMetadata.setReferences(null);
  }
}
