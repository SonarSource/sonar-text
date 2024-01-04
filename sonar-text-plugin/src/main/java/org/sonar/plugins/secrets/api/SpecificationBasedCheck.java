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

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.function.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.plugins.common.Check;
import org.sonar.plugins.common.DurationStatistics;
import org.sonar.plugins.common.InputFileContext;
import org.sonar.plugins.secrets.SecretsRulesDefinition;
import org.sonar.plugins.secrets.configuration.model.Rule;

/**
 * A base Check class for all checks that are configured by YAML specification.
 */
public abstract class SpecificationBasedCheck extends Check {

  private static final Logger LOG = LoggerFactory.getLogger(SpecificationBasedCheck.class);
  private List<SecretMatcher> matcher;

  // shared map between all specificationBasedChecks used to calculate if secret was already reported for textRange
  private Map<URI, List<TextRange>> reportedIssuesForCtx;
  private DurationStatistics durationStatistics;

  protected SpecificationBasedCheck() {
    super();
  }

  @Override
  protected String repositoryKey() {
    return SecretsRulesDefinition.REPOSITORY_KEY;
  }

  /**
   * Initialize this check by loading rule definitions
   *
   * @param loader               SpecificationLoader that will provide specification files
   * @param reportedIssuesForCtx a map to store reported issues to prevent duplication among different checks
   * @param durationStatistics   an instance to record performance statistics
   */
  public void initialize(SpecificationLoader loader, Map<URI, List<TextRange>> reportedIssuesForCtx, DurationStatistics durationStatistics) {
    this.reportedIssuesForCtx = reportedIssuesForCtx;
    this.durationStatistics = durationStatistics;
    String ruleId = getRuleKey().rule();
    List<Rule> rulesForKey = retrieveRules(loader, ruleId);
    if (rulesForKey.isEmpty()) {
      LOG.error("Found no rule specification for rule with key: {}", ruleId);
    }
    this.matcher = rulesForKey.stream()
      .map(rule -> SecretMatcher.build(rule, durationStatistics))
      .collect(Collectors.toList());
  }

  /**
   * Loads rules.
   *
   * @param loader  the {@link SpecificationLoader loader} of the rules
   * @param ruleKey the Rule Key
   * @return list of {@link Rule rules}
   */
  public List<Rule> retrieveRules(SpecificationLoader loader, String ruleKey) {
    return loader.getRulesForKey(ruleKey);
  }

  @Override
  public void analyze(InputFileContext ctx) {
    analyze(ctx, checkId -> true);
  }

  /**
   * Analyses a specific rule.
   *
   * @param ctx    the {@link InputFileContext input file context}
   * @param ruleId the Rule ID
   */
  public void analyze(InputFileContext ctx, String ruleId) {
    analyze(ctx, checkId -> checkId.equals(ruleId));
  }

  protected void analyze(InputFileContext ctx, Predicate<String> ruleFilter) {
    for (SecretMatcher secretMatcher : matcher) {
      if (ruleFilter.test(secretMatcher.getRuleId())) {
        durationStatistics.timed(secretMatcher.getRuleId() + DurationStatistics.SUFFIX_TOTAL, () -> secretMatcher.findIn(ctx))
          .stream()
          .map(match -> ctx.newTextRangeFromFileOffsets(match.getFileStartOffset(), match.getFileEndOffset()))
          .forEach(textRange -> reportIfNoOverlappingSecretAlreadyFound(ctx, textRange, secretMatcher));
      }
    }
  }

  /**
   * Reports a found secret. The secret is not reported if another {@link SecretMatcher} reported it already
   * (to avoid duplications on the same secret).
   *
   * @param ctx           the {@link InputFileContext input file context}
   * @param foundSecret   the {@link TextRange text range} of found secret
   * @param secretMatcher the {@link SecretMatcher} that found it
   */
  public void reportIfNoOverlappingSecretAlreadyFound(InputFileContext ctx, TextRange foundSecret, SecretMatcher secretMatcher) {
    List<TextRange> reportedSecrets = reportedIssuesForCtx.compute(ctx.getInputFile().uri(), (k, v) -> Objects.requireNonNullElseGet(v, ArrayList::new));

    boolean noOverlappingSecrets = reportedSecrets.stream().noneMatch(reportedSecret -> reportedSecret.overlap(foundSecret));
    if (noOverlappingSecrets) {
      reportedSecrets.add(foundSecret);
      ctx.reportIssue(getRuleKey(), foundSecret, secretMatcher.getMessageFromRule());
    }
  }

  public List<SecretMatcher> getMatcher() {
    return matcher;
  }

}
