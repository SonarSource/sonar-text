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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import org.sonar.plugins.common.InputFileContext;
import org.sonar.plugins.common.measures.DurationStatistics;
import org.sonar.plugins.secrets.configuration.model.Rule;
import org.sonar.plugins.secrets.configuration.model.Selectivity;
import org.sonar.plugins.secrets.configuration.model.matching.filter.NamedPostModule;

import static java.util.stream.Collectors.toMap;

/**
 * Checks if the file contains some secrets.
 */
public class SecretMatcher implements Matcher {

  private final String ruleId;
  private final Selectivity ruleSelectivity;
  private final String ruleMessage;
  private final PatternMatcher patternMatcher;
  private final AuxiliaryPatternMatcher auxiliaryPatternMatcher;
  private final Predicate<InputFileContext> preFilter;
  private final Predicate<String> postFilter;
  private final Map<String, Predicate<String>> postFilterByGroup;

  private final DurationStatistics durationStatistics;

  SecretMatcher(String ruleId,
    String ruleMessage,
    Selectivity ruleSelectivity,
    PatternMatcher patternMatcher,
    AuxiliaryPatternMatcher auxiliaryPatternMatcher,
    Predicate<InputFileContext> preFilter,
    Predicate<String> postFilter,
    Map<String, Predicate<String>> postFilterByGroup,
    DurationStatistics durationStatistics) {
    this.ruleId = ruleId;
    this.ruleSelectivity = ruleSelectivity;
    this.ruleMessage = ruleMessage;
    this.patternMatcher = patternMatcher;
    this.auxiliaryPatternMatcher = auxiliaryPatternMatcher;
    this.preFilter = preFilter;
    this.postFilter = postFilter;
    this.postFilterByGroup = postFilterByGroup;
    this.durationStatistics = durationStatistics;
  }

  /**
   * Creates a new SecretMatcher from provided Rule
   *
   * @param rule                       rule to extract matcher logic from
   * @param durationStatistics         instance to collect performance statistics
   * @param specificationConfiguration configuration of specification
   * @return a new SecretMatcher
   */
  public static SecretMatcher build(Rule rule, DurationStatistics durationStatistics, SpecificationConfiguration specificationConfiguration) {
    var patternMatcher = PatternMatcher.build(rule.getDetection().getMatching());
    Predicate<InputFileContext> preFilter = PreFilterFactory.createPredicate(rule.getDetection().getPre(), specificationConfiguration);
    Predicate<String> postFilter = PostFilterFactory.createPredicate(rule.getDetection().getPost());
    var postFilterByGroup = Optional.ofNullable(rule.getDetection().getPost()).stream()
      .flatMap(it -> it.getGroups().stream())
      .collect(toMap(NamedPostModule::getName, PostFilterFactory::createPredicate));

    var auxiliaryMatcher = AuxiliaryPatternMatcherFactory.build(rule.getDetection().getMatching());
    return new SecretMatcher(
      rule.getId(),
      rule.getMetadata().getMessage(),
      rule.getSelectivity(),
      patternMatcher,
      auxiliaryMatcher,
      preFilter,
      postFilter,
      postFilterByGroup,
      durationStatistics);
  }

  @Override
  public List<Match> findIn(InputFileContext fileContext) {
    boolean isRejectedOnPreFilter = durationStatistics.timed(
      getRuleId() + DurationStatistics.SUFFIX_PRE,
      () -> !preFilter.test(fileContext));
    if (isRejectedOnPreFilter) {
      return Collections.emptyList();
    }

    String content = fileContext.content();
    List<Match> secretsFilteredOnContext = durationStatistics.timed(
      getRuleId() + DurationStatistics.SUFFIX_MATCHER,
      () -> {
        List<Match> candidateSecrets = patternMatcher.findIn(content, getRuleId(), postFilterByGroup.keySet());
        return auxiliaryPatternMatcher.filter(candidateSecrets, fileContext, getRuleId());
      });

    return secretsFilteredOnContext.stream()
      .filter(match -> durationStatistics.timed(
        getRuleId() + DurationStatistics.SUFFIX_POST,
        () -> postFilter.test(match.text()) && match.groups().entrySet().stream()
          .allMatch(entry -> postFilterByGroup.getOrDefault(entry.getKey(), s -> true).test(entry.getValue().text()))))
      .toList();
  }

  public String getRuleId() {
    return ruleId;
  }

  public String getMessageFromRule() {
    return ruleMessage;
  }

  Predicate<String> getPostFilter() {
    return postFilter;
  }

  public Map<String, Predicate<String>> getPostFilterByGroup() {
    return postFilterByGroup;
  }

  AuxiliaryPatternMatcher getAuxiliaryPatternMatcher() {
    return auxiliaryPatternMatcher;
  }

  public Selectivity getRuleSelectivity() {
    return ruleSelectivity;
  }
}
