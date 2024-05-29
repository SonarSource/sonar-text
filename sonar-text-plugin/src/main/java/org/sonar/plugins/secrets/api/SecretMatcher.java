/*
 * SonarQube Text Plugin
 * Copyright (C) 2021-2024 SonarSource SA
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
import java.util.List;
import java.util.function.Predicate;
import org.sonar.plugins.common.DurationStatistics;
import org.sonar.plugins.common.InputFileContext;
import org.sonar.plugins.secrets.configuration.model.Rule;

/**
 * Checks if the file contains some secrets.
 */
public class SecretMatcher {

  private final String ruleId;
  private final String ruleMessage;
  private final PatternMatcher patternMatcher;
  private final AuxiliaryPatternMatcher auxiliaryPatternMatcher;
  private final Predicate<InputFileContext> preFilter;
  private final Predicate<String> postFilter;
  private final DurationStatistics durationStatistics;

  SecretMatcher(String ruleId,
    String ruleMessage,
    PatternMatcher patternMatcher,
    AuxiliaryPatternMatcher auxiliaryPatternMatcher,
    Predicate<InputFileContext> preFilter,
    Predicate<String> postFilter,
    DurationStatistics durationStatistics) {
    this.ruleId = ruleId;
    this.ruleMessage = ruleMessage;
    this.patternMatcher = patternMatcher;
    this.auxiliaryPatternMatcher = auxiliaryPatternMatcher;
    this.preFilter = preFilter;
    this.postFilter = postFilter;
    this.durationStatistics = durationStatistics;
  }

  /**
   * Creates a new SecretMatcher from provided Rule
   *
   * @param rule               rule to extract matcher logic from
   * @param durationStatistics instance to collect performance statistics
   * @return a new SecretMatcher
   */
  public static SecretMatcher build(Rule rule, DurationStatistics durationStatistics) {
    var patternMatcher = PatternMatcher.build(rule.getDetection().getMatching());
    Predicate<InputFileContext> preFilter = PreFilterFactory.createPredicate(rule.getDetection().getPre());
    Predicate<String> postFilter = PostFilterFactory.createPredicate(rule.getDetection().getPost(), rule.getDetection().getMatching());
    var auxiliaryMatcher = AuxiliaryPatternMatcherFactory.build(rule.getDetection().getMatching());
    return new SecretMatcher(rule.getId(), rule.getMetadata().getMessage(), patternMatcher, auxiliaryMatcher, preFilter, postFilter, durationStatistics);
  }

  /**
   * Returns a list of {@link Match matches} found in {@link InputFileContext}.
   *
   * @param fileContext the file that will be scanned.
   * @return list of matches.
   */
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
        List<Match> candidateSecrets = patternMatcher.findIn(content, getRuleId());
        return auxiliaryPatternMatcher.filter(candidateSecrets, fileContext, getRuleId());
      });

    return secretsFilteredOnContext.stream()
      .filter(match -> durationStatistics.timed(
        getRuleId() + DurationStatistics.SUFFIX_POST,
        () -> postFilter.test(match.getText())))
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

  AuxiliaryPatternMatcher getAuxiliaryPatternMatcher() {
    return auxiliaryPatternMatcher;
  }
}
