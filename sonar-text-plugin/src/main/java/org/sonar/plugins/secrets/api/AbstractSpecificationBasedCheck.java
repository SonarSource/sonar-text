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
import org.sonar.plugins.common.Check;
import org.sonar.plugins.common.DurationStatistics;
import org.sonar.plugins.common.InputFileContext;

/**
 * A base Check class for all checks that load specification from files.
 * @param <L> SpecificationLoader that will provide specification files
 * @param <M> Matcher that will be used to find matches in the input file
 */
public abstract class AbstractSpecificationBasedCheck<L extends SpecificationLoader, M extends Matcher> extends Check {

  protected List<M> matchers;
  protected DurationStatistics durationStatistics;

  protected AbstractSpecificationBasedCheck() {
    super();
  }

  /**
   * Initialize this check by loading rule definitions
   *
   * @param loader                     SpecificationLoader that will provide specification files
   * @param durationStatistics         an instance to record performance statistics
   * @param specificationConfiguration configuration of specification
   */
  public void initialize(L loader, DurationStatistics durationStatistics, SpecificationConfiguration specificationConfiguration) {
    this.durationStatistics = durationStatistics;
    String ruleId = getRuleKey().rule();
    this.matchers = initializeMatchers(loader, ruleId, specificationConfiguration);
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

  protected abstract void analyze(InputFileContext ctx, Predicate<String> ruleFilter);

  protected abstract List<M> initializeMatchers(L loader, String ruleId, SpecificationConfiguration specificationConfiguration);
}
