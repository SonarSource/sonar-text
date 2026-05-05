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
package org.sonar.plugins.secrets;

import org.sonar.plugins.common.Check;
import org.sonar.plugins.common.git.GitService;
import org.sonar.plugins.common.git.NullGitService;
import org.sonar.plugins.secrets.api.SpecificationConfiguration;
import org.sonar.plugins.secrets.api.filters.PreFilter;
import org.sonar.plugins.secrets.api.filters.PreFilterFactory;
import org.sonar.plugins.secrets.api.filters.SkippedFilter;

public abstract class AbstractBinaryFileCheck extends Check {

  protected PreFilter scopedFilePreFilter;
  protected GitService gitService = NullGitService.INSTANCE;

  /**
   * Initialize this check by creating a scope-based {@link PreFilter} and providing a {@link GitService} instance.
   * This method should be called by the sensor before analyzing files.
   *
   * <p>The {@link PreFilter} is built via {@link PreFilterFactory#createFilter} so binary checks honor
   * {@link SkippedFilter#TEST_FILES_FILTER} the same way spec-based checks do: when the filter is in the
   * skipped-filter set, auto-detected test files pass through with the skipped marker so callers can tag
   * the issue message as low-confidence.
   *
   * @param specificationConfiguration test-file detection settings, skipped filters, and message formatting
   * @param gitService {@link GitService} instance to be used by checks like S7203
   */
  public void initialize(SpecificationConfiguration specificationConfiguration, GitService gitService) {
    this.scopedFilePreFilter = PreFilterFactory.createFilter(null, null, specificationConfiguration, true);
    this.gitService = gitService;
  }

  @Override
  protected String repositoryKey() {
    return SecretsRulesDefinition.REPOSITORY_KEY;
  }
}
