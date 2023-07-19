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
import org.sonar.plugins.common.InputFileContext;
import org.sonar.plugins.secrets.configuration.model.Rule;

public class SecretMatcher {

  private final Rule rule;
  private final PatternMatcher patternMatcher;

  private final Predicate<InputFileContext> preFilter;

  private final Predicate<String> postFilter;

  SecretMatcher(Rule rule, PatternMatcher patternMatcher, Predicate<InputFileContext> preFilter, Predicate<String> postFilter) {
    this.rule = rule;
    this.patternMatcher = patternMatcher;
    this.preFilter = preFilter;
    this.postFilter = postFilter;
  }

  public static SecretMatcher build(Rule rule) {
    PatternMatcher matcher = PatternMatcher.build(rule.getDetection().getMatching());
    Predicate<InputFileContext> preFilter = PreFilterFactory.createPredicate(rule.getDetection().getPre());
    Predicate<String> postFilter = PostFilterFactory.createPredicate(rule.getDetection().getPost());
    return new SecretMatcher(rule, matcher, preFilter, postFilter);
  }

  public List<Match> findIn(InputFileContext fileContext) {
    if (!getPreFilter().test(fileContext)) {
      return Collections.emptyList();
    }

    return patternMatcher.findIn(fileContext.content()).stream()
      .filter(match -> postFilter.test(match.getText())).collect(Collectors.toList());
  }

  public String getMessageFromRule() {
    return rule.getMetadata().getMessage();
  }

  public Predicate<InputFileContext> getPreFilter() {
    return preFilter;
  }

  public Predicate<String> getPostFilter() {
    return postFilter;
  }
}
