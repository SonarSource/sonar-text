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

import java.util.Set;
import java.util.function.Supplier;
import org.sonar.plugins.secrets.api.filters.RejectionLogger;
import org.sonar.plugins.secrets.api.filters.SkippedFilter;

/**
 * The settings for {@link SpecificationBasedCheck} initialization.
 *
 * @param automaticTestFileDetectionEnabled master switch for the heuristic (filename/path) test-file detection. It is
 *                                    enabled only when the project does not declare its own test files (e.g.
 *                                    {@code sonar.tests} is not set); when the project declares them, this is
 *                                    {@code false} and the heuristic is never applied. It only decides whether a file
 *                                    is <em>treated as</em> an automatically-detected test file — what happens to such
 *                                    a file (skipped from analysis by default, or analyzed and flagged low-confidence)
 *                                    is governed separately by {@link SkippedFilter#TEST_FILES_FILTER} in
 *                                    {@code skippedFilters}.
 * @param skippedFilters              immutable set of filters to skip as requested by the caller; each skipped filter
 *                                    lets a specific filter pass candidates it would otherwise reject, with affected
 *                                    findings tagged accordingly.
 * @param messageFormatter            formatter for secret issue messages.
 * @param rejectionLogger             logger used by post-filters to emit debug lines when a candidate is rejected.
 *                                    Pass {@link RejectionLogger#DISABLED} (the default) to opt out.
 * @param genericSecretRuleCoverageSupplier   supplies the current analysis's language-rule coverage. In SonarQube for IDE (SQIDE),
 *                                    the module-scoped {@link org.sonar.plugins.secrets.utils.CheckContainer} retains checks and
 *                                    matchers across analyses. A supplier lets those cached matchers read updated coverage
 *                                    instead of retaining the first analysis's snapshot.
 */
public record SpecificationConfiguration(
  boolean automaticTestFileDetectionEnabled,
  Set<SkippedFilter> skippedFilters,
  MessageFormatter messageFormatter,
  RejectionLogger rejectionLogger,
  Supplier<GenericSecretRuleCoverage> genericSecretRuleCoverageSupplier) {

  /** Resolves coverage when a pre-filter runs so cached matchers observe the current analysis. */
  public GenericSecretRuleCoverage genericSecretRuleCoverage() {
    return genericSecretRuleCoverageSupplier.get();
  }

  /** Creates a configuration for callers without competing language analyzers, such as the standalone CLI. */
  public SpecificationConfiguration(boolean automaticTestFileDetectionEnabled, Set<SkippedFilter> skippedFilters,
    MessageFormatter messageFormatter, RejectionLogger rejectionLogger) {
    this(automaticTestFileDetectionEnabled, skippedFilters, messageFormatter, rejectionLogger, () -> GenericSecretRuleCoverage.EMPTY);
  }

  public static final SpecificationConfiguration AUTO_TEST_FILE_DETECTION_ENABLED = new SpecificationConfiguration(true, Set.of(), MessageFormatter.RULE_MESSAGE,
    RejectionLogger.DISABLED);
  public static final SpecificationConfiguration AUTO_TEST_FILE_DETECTION_DISABLED = new SpecificationConfiguration(false, Set.of(), MessageFormatter.RULE_MESSAGE,
    RejectionLogger.DISABLED);
}
