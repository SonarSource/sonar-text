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

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.sonar.plugins.common.DurationStatistics;
import org.sonar.plugins.common.InputFileContext;
import org.sonar.plugins.secrets.configuration.model.Rule;

public class SecretMatcher {

  private final Rule rule;
  private final PatternMatcher patternMatcher;
  private final AuxiliaryPatternMatcher auxiliaryPatternMatcher;
  private final Predicate<InputFileContext> preFilter;
  private final Predicate<String> postFilter;
  private final DurationStatistics ds;

  SecretMatcher(Rule rule,
    PatternMatcher patternMatcher,
    AuxiliaryPatternMatcher auxiliaryPatternMatcher,
    Predicate<InputFileContext> preFilter,
    Predicate<String> postFilter,
    DurationStatistics ds) {
    this.rule = rule;
    this.patternMatcher = patternMatcher;
    this.auxiliaryPatternMatcher = auxiliaryPatternMatcher;
    this.preFilter = preFilter;
    this.postFilter = postFilter;
    this.ds = ds;
  }

  public static SecretMatcher build(Rule rule, DurationStatistics durationStatistics) {
    PatternMatcher patternMatcher = PatternMatcher.build(rule.getDetection().getMatching());
    Predicate<InputFileContext> preFilter = PreFilterFactory.createPredicate(rule.getDetection().getPre());
    Predicate<String> postFilter = PostFilterFactory.createPredicate(rule.getDetection().getPost(), rule.getDetection().getMatching());
    AuxiliaryPatternMatcher auxiliaryMatcher = AuxiliaryPatternMatcherFactory.build(rule.getDetection().getMatching());
    return new SecretMatcher(rule, patternMatcher, auxiliaryMatcher, preFilter, postFilter, durationStatistics);
  }

  public List<Match> findIn(InputFileContext fileContext) {
    boolean isPreFilterMatching = ds.timed(getRuleId() + "-preFilter", () -> preFilter.test(fileContext));
    if (!isPreFilterMatching) {
      return Collections.emptyList();
    }

    String content = fileContext.content();
    List<Match> secretsFilteredOnContext = ds.timed(getRuleId() + "-matcher", () -> {
      List<Match> candidateSecrets = patternMatcher.findIn(content);
      return auxiliaryPatternMatcher.filter(candidateSecrets, content);
    });

    return secretsFilteredOnContext.stream()
      .filter(match -> ds.timed(getRuleId() + "-postFilter", () -> postFilter.test(match.getText())))
      .collect(Collectors.toList());
  }

  public String getRuleId() {
    return rule.getId();
  }

  public String getMessageFromRule() {
    return rule.getMetadata().getMessage();
  }

  Predicate<String> getPostFilter() {
    return postFilter;
  }

  AuxiliaryPatternMatcher getAuxiliaryPatternMatcher() {
    return auxiliaryPatternMatcher;
  }
}
