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

import java.util.List;
import java.util.function.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.plugins.common.DurationStatistics;
import org.sonar.plugins.common.InputFileContext;
import org.sonar.plugins.secrets.SecretsRulesDefinition;
import org.sonar.plugins.secrets.configuration.model.Rule;

/**
 * A base Check class for all checks that are configured by YAML specification.
 */
public abstract class SpecificationBasedCheck extends AbstractSpecificationBasedCheck<SecretsSpecificationLoader, SecretMatcher> {

  private static final Logger LOG = LoggerFactory.getLogger(SpecificationBasedCheck.class);

  protected SpecificationBasedCheck() {
    super();
  }

  @Override
  protected String repositoryKey() {
    return SecretsRulesDefinition.REPOSITORY_KEY;
  }

  /**
   * Loads rules.
   *
   * @param loader  the {@link SecretsSpecificationLoader loader} of the rules
   * @param ruleKey the Rule Key
   * @return list of {@link Rule rules}
   */
  public List<Rule> retrieveRules(SecretsSpecificationLoader loader, String ruleKey) {
    return loader.getRulesForKey(ruleKey);
  }

  protected void analyze(InputFileContext ctx, Predicate<String> ruleFilter) {
    for (SecretMatcher secretMatcher : matchers) {
      if (ruleFilter.test(secretMatcher.getRuleId())) {
        durationStatistics.timed(secretMatcher.getRuleId() + DurationStatistics.SUFFIX_TOTAL, () -> secretMatcher.findIn(ctx))
          .stream()
          .map(match -> ctx.newTextRangeFromFileOffsets(match.getFileStartOffset(), match.getFileEndOffset()))
          .forEach(textRange -> ctx.reportSecretIssue(getRuleKey(), textRange, secretMatcher.getMessageFromRule()));
      }
    }
  }

  @Override
  protected List<SecretMatcher> initializeMatchers(SecretsSpecificationLoader loader, String ruleId, SpecificationConfiguration specificationConfiguration) {
    List<Rule> rulesForKey = retrieveRules(loader, ruleId);
    if (rulesForKey.isEmpty()) {
      LOG.warn("Found no rule specification for rule with key: {}", ruleId);
    }
    return rulesForKey.stream()
      .map(rule -> SecretMatcher.build(rule, durationStatistics, specificationConfiguration))
      .toList();
  }
}
