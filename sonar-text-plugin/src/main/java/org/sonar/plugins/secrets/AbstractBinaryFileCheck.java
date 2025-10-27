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
package org.sonar.plugins.secrets;

import java.util.function.Predicate;
import org.sonar.plugins.common.Check;
import org.sonar.plugins.common.InputFileContext;
import org.sonar.plugins.common.git.GitService;
import org.sonar.plugins.common.git.NullGitService;
import org.sonar.plugins.secrets.api.SpecificationConfiguration;

import static org.sonar.plugins.secrets.api.PreFilterFactory.INCLUDE_ONLY_MAIN_FILES;
import static org.sonar.plugins.secrets.api.PreFilterFactory.appendAutomaticNoTestFileFilter;

public abstract class AbstractBinaryFileCheck extends Check {

  protected Predicate<InputFileContext> scopedFilePredicate;
  protected GitService gitService = NullGitService.INSTANCE;

  /**
   * Initialize this check by creating a scope based file predicate and providing a {@link GitService} instance.
   * This method should be called by the sensor before analyzing files.
   *
   * @param specificationConfiguration configuration if test files should be automatically detected
   * @param gitService {@link GitService} instance to be used by checks like S7203
   */
  public void initialize(SpecificationConfiguration specificationConfiguration, GitService gitService) {
    this.scopedFilePredicate = appendAutomaticNoTestFileFilter(INCLUDE_ONLY_MAIN_FILES, specificationConfiguration);
    this.gitService = gitService;
  }

  @Override
  protected String repositoryKey() {
    return SecretsRulesDefinition.REPOSITORY_KEY;
  }
}
