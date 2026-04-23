/*
 * SonarQube Text Plugin
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.sonar.plugins.common.InputFileContext;
import org.sonar.plugins.common.measures.DurationStatistics;
import org.sonar.plugins.secrets.api.filters.FilterOutcome;
import org.sonar.plugins.secrets.api.filters.PostFilter;
import org.sonar.plugins.secrets.api.filters.PostFilterFactory;
import org.sonar.plugins.secrets.api.filters.PreFilterFactory;
import org.sonar.plugins.secrets.api.filters.SkippedFilter;
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
  private final PostFilter postFilter;
  private final Map<String, PostFilter> postFilterByGroup;

  private final DurationStatistics durationStatistics;

  SecretMatcher(String ruleId,
    String ruleMessage,
    Selectivity ruleSelectivity,
    PatternMatcher patternMatcher,
    AuxiliaryPatternMatcher auxiliaryPatternMatcher,
    Predicate<InputFileContext> preFilter,
    PostFilter postFilter,
    Map<String, PostFilter> postFilterByGroup,
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
   * @param shouldExecuteContentPreFilters    whether content pre-filters should be executed or they have been executed earlier
   * @return a new SecretMatcher
   */
  public static SecretMatcher build(
    Rule rule,
    DurationStatistics durationStatistics,
    SpecificationConfiguration specificationConfiguration,
    boolean shouldExecuteContentPreFilters) {
    var patternMatcher = PatternMatcher.build(rule.getDetection().getMatching());
    Predicate<InputFileContext> preFilter = PreFilterFactory.createPredicate(
      rule.getDetection().getPre(), rule.getSelectivity(), specificationConfiguration, shouldExecuteContentPreFilters);
    Set<SkippedFilter> skippedFilters = specificationConfiguration.skippedFilters();
    PostFilter postFilter = PostFilterFactory.createFilter(rule.getDetection().getPost(), skippedFilters);
    var postFilterByGroup = Optional.ofNullable(rule.getDetection().getPost()).stream()
      .flatMap(it -> it.getGroups().stream())
      .collect(toMap(NamedPostModule::getName, namedPost -> PostFilterFactory.createFilter(namedPost, skippedFilters)));

    var auxiliaryMatcher = AuxiliaryPatternMatcherFactory.build(rule.getDetection().getMatching());
    var ruleMessage = specificationConfiguration.messageFormatter().format(rule.getMetadata());
    return new SecretMatcher(
      rule.getId(),
      ruleMessage,
      rule.getSelectivity(),
      patternMatcher,
      auxiliaryMatcher,
      preFilter,
      postFilter,
      postFilterByGroup,
      durationStatistics);
  }

  @Override
  public List<MatchResult> findIn(InputFileContext fileContext) {
    boolean isRejectedOnPreFilter = durationStatistics.timed(
      getRuleId() + DurationStatistics.SUFFIX_PRE,
      () -> !preFilter.test(fileContext));
    if (isRejectedOnPreFilter) {
      return Collections.emptyList();
    }

    String content = fileContext.content();
    List<CandidateMatch> secretsFilteredOnContext = durationStatistics.timed(
      getRuleId() + DurationStatistics.SUFFIX_MATCHER,
      () -> {
        List<CandidateMatch> candidateSecrets = patternMatcher.findMatches(content, getRuleId(), postFilterByGroup.keySet());
        return auxiliaryPatternMatcher.filter(candidateSecrets, fileContext, getRuleId());
      });

    return secretsFilteredOnContext.stream()
      .map(match -> new MatchResult(match, durationStatistics.timed(
        getRuleId() + DurationStatistics.SUFFIX_POST,
        () -> applyPostFilters(match))))
      .filter(accepted -> accepted.outcome().passed())
      .toList();
  }

  private FilterOutcome applyPostFilters(CandidateMatch match) {
    FilterOutcome outcome = postFilter.apply(match.text());
    if (!outcome.passed()) {
      return outcome;
    }
    for (var entry : match.groups().entrySet()) {
      PostFilter groupFilter = postFilterByGroup.getOrDefault(entry.getKey(), PostFilter.ACCEPT_ALL);
      outcome = outcome.combine(groupFilter.apply(entry.getValue().text()));
    }
    return outcome;
  }

  public String getRuleId() {
    return ruleId;
  }

  /**
   * Returns the issue message for a specific filtered match. When any filter was skipped, the message is
   * amended with a suffix listing the reasons (composed from {@link SkippedFilter#lowConfidenceLabel}) so the finding
   * is surfaced as low-confidence.
   *
   * @param outcome the filter outcome for a candidate match
   * @return the issue message, potentially amended with a low-confidence suffix
   */
  public String getMessageForCandidate(FilterOutcome outcome) {
    Set<SkippedFilter> skipped = outcome.skipped();
    if (skipped.isEmpty()) {
      return ruleMessage;
    }
    String reasons = skipped.stream()
      .map(SkippedFilter::lowConfidenceLabel)
      .collect(Collectors.joining(", "));
    return ruleMessage + " (" + reasons + ")";
  }

  PostFilter getPostFilter() {
    return postFilter;
  }

  public Map<String, PostFilter> getPostFilterByGroup() {
    return postFilterByGroup;
  }

  AuxiliaryPatternMatcher getAuxiliaryPatternMatcher() {
    return auxiliaryPatternMatcher;
  }

  public Selectivity getRuleSelectivity() {
    return ruleSelectivity;
  }
}
