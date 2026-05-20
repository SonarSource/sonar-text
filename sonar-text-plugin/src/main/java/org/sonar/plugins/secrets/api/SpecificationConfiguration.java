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
import org.sonar.plugins.secrets.api.filters.RejectionLogger;
import org.sonar.plugins.secrets.api.filters.SkippedFilter;

/**
 * The settings for {@link SpecificationBasedCheck} initialization.
 *
 * @param automaticTestFileDetection enable the automatic test file detection.
 * @param skippedFilters             immutable set of filters to skip as requested by the caller; each skipped filter
 *                                   lets a specific filter pass candidates it would otherwise reject, with affected
 *                                   findings tagged accordingly.
 * @param messageFormatter           formatter for secret issue messages.
 * @param rejectionLogger            logger used by post-filters to emit debug lines when a candidate is rejected.
 *                                   Pass {@link RejectionLogger#DISABLED} (the default) to opt out.
 */
public record SpecificationConfiguration(
  boolean automaticTestFileDetection,
  Set<SkippedFilter> skippedFilters,
  MessageFormatter messageFormatter,
  RejectionLogger rejectionLogger) {

  public SpecificationConfiguration(boolean automaticTestFileDetection, Set<SkippedFilter> skippedFilters, MessageFormatter messageFormatter) {
    this(automaticTestFileDetection, skippedFilters, messageFormatter, RejectionLogger.DISABLED);
  }

  public SpecificationConfiguration(boolean automaticTestFileDetection) {
    this(automaticTestFileDetection, Set.of(), MessageFormatter.RULE_MESSAGE, RejectionLogger.DISABLED);
  }

  public static final SpecificationConfiguration AUTO_TEST_FILE_DETECTION_ENABLED = new SpecificationConfiguration(true);
  public static final SpecificationConfiguration AUTO_TEST_FILE_DETECTION_DISABLED = new SpecificationConfiguration(false);
}
