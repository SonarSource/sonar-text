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
import org.sonar.plugins.secrets.configuration.model.matching.AuxiliaryPattern;
import org.sonar.plugins.secrets.configuration.model.matching.AuxiliaryPatternType;
import org.sonar.plugins.secrets.configuration.model.matching.BooleanCombination;
import org.sonar.plugins.secrets.configuration.model.matching.BooleanCombinationType;
import org.sonar.plugins.secrets.configuration.model.matching.Detection;
import org.sonar.plugins.secrets.configuration.model.matching.Match;
import org.sonar.plugins.secrets.configuration.model.matching.Matching;
import org.sonar.plugins.secrets.configuration.model.matching.filter.HeuristicsFilter;
import org.sonar.plugins.secrets.configuration.model.matching.filter.IncludedFilter;
import org.sonar.plugins.secrets.configuration.model.matching.filter.PostModule;
import org.sonar.plugins.secrets.configuration.model.matching.filter.PreModule;
import org.sonar.plugins.secrets.configuration.model.matching.filter.RejectFilter;
import org.sonar.plugins.secrets.configuration.model.matching.filter.StatisticalFilter;
import org.sonar.plugins.secrets.configuration.model.metadata.Metadata;
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
    providerMetadata.setMessage("provider message");
    providerMetadata.setCategory("Cloud provider");
    providerMetadata.setName("provider name");
    return providerMetadata;
  }

  public static Rule constructRule() {
    Rule rule = new Rule();
    rule.setId("exampleKey");
    rule.setMetadata(constructRuleMetadata());
    rule.setDetection(constructBasicDetection("\\b(rule matching pattern)\\b"));
    rule.setExamples(List.of(constructRuleExample()));
    return rule;
  }

  private static RuleMetadata constructRuleMetadata() {
    RuleMetadata ruleMetadata = new RuleMetadata();
    ruleMetadata.setName("rule name");
    return ruleMetadata;
  }

  public static Detection constructBasicDetection(String pattern) {
    Detection detection = new Detection();
    Matching matching = new Matching();

    matching.setPattern(pattern);
    detection.setMatching(matching);

    return detection;
  }

  public static AuxiliaryPattern constructAuxiliaryPattern(AuxiliaryPatternType type, String pattern) {
    AuxiliaryPattern auxiliaryPattern = new AuxiliaryPattern();
    auxiliaryPattern.setType(type);
    auxiliaryPattern.setPattern(pattern);
    return auxiliaryPattern;
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
    specification.getProvider().setDetection(constructBasicDetection("\\b(provider matching pattern)\\b"));

    return specification;
  }

  private static void enrichRule(Rule rule) {
    enrichRuleMetadata(rule.getMetadata());
    enrichRuleExample(rule.getExamples().get(0));
    enrichDetection(rule.getDetection());
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

  public static void enrichDetection(Detection detection) {
    detection.setPre(constructPreModule());
    detection.setPost(constructPostModule());

    BooleanCombination matchEach = new BooleanCombination();
    matchEach.setType(BooleanCombinationType.MATCH_EACH);
    matchEach.setMatches(List.of(
      constructAuxiliaryPattern(AuxiliaryPatternType.PATTERN_AFTER, "pattern-after"),
      constructAuxiliaryPattern(AuxiliaryPatternType.PATTERN_AROUND, "pattern-around")
    ));

    BooleanCombination matchEitherLevelTwo = new BooleanCombination();
    matchEitherLevelTwo.setType(BooleanCombinationType.MATCH_EITHER);
    matchEitherLevelTwo.setMatches(List.of(
      constructAuxiliaryPattern(AuxiliaryPatternType.PATTERN_NOT, "pattern-not"),
      constructAuxiliaryPattern(AuxiliaryPatternType.PATTERN_AROUND, "pattern-around")
    ));

    List<Match> matches = new ArrayList<>();
    matches.add(constructAuxiliaryPattern(AuxiliaryPatternType.PATTERN_BEFORE, "pattern-before"));
    matches.add(matchEach);
    matches.add(matchEitherLevelTwo);

    BooleanCombination matchEither = new BooleanCombination();
    matchEither.setType(BooleanCombinationType.MATCH_EITHER);
    matchEither.setMatches(matches);

    detection.getMatching().setContext(matchEither);
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
    statisticalFilter.setThreshold(4.2f);

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
  public static void setSpecificMetadataFieldsNull(Metadata metadata) {
    metadata.setImpact(null);
    metadata.setFix(null);
    metadata.setMessage(null);
    metadata.setReferences(null);
  }

  public static void setDetectionFieldsNull(Detection detection) {
    detection.setMatching(null);
    detection.setPre(null);
    detection.setPost(null);
  }

}
