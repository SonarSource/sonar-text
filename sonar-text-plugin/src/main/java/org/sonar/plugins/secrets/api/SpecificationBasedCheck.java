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

import java.util.List;
import java.util.function.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.plugins.common.InputFileContext;
import org.sonar.plugins.common.measures.DurationStatistics;
import org.sonar.plugins.secrets.SecretsRulesDefinition;
import org.sonar.plugins.secrets.configuration.model.Rule;

import static org.sonar.plugins.secrets.utils.ContentPreFilterUtils.hasOptimizableContentPrefilters;

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
          .map(match -> ctx.newTextRangeFromFileOffsets(match.fileStartOffset(), match.fileEndOffset()))
          .forEach(textRange -> ctx.reportIssueOnTextRange(getRuleKey(), secretMatcher.getRuleSelectivity(), textRange, secretMatcher.getMessageFromRule()));
      }
    }
  }

  @Override
  protected List<SecretMatcher> initializeMatchers(SecretsSpecificationLoader loader, String ruleId, SpecificationConfiguration specificationConfiguration) {
    List<Rule> rulesForKey = retrieveRules(loader, ruleId);
    if (rulesForKey.isEmpty()) {
      LOG.warn("Found no rule specification for rule with key: {}", ruleId);
    }
    // Only if there is a single (and optimizable) rule for the key, we can safely skip execution of content pre-filters.
    // See SONARTEXT-585 for more details.
    var shouldExecuteContentPreFilters = !(rulesForKey.size() == 1 && hasOptimizableContentPrefilters(rulesForKey));
    return rulesForKey.stream()
      .map(rule -> SecretMatcher.build(rule, durationStatistics, specificationConfiguration, shouldExecuteContentPreFilters))
      .toList();
  }
}
