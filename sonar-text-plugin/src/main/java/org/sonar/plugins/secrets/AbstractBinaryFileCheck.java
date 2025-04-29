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

import java.util.List;
import java.util.function.Predicate;
import org.sonar.plugins.common.Check;
import org.sonar.plugins.common.InputFileContext;
import org.sonar.plugins.secrets.api.ScopeBasedFileFilter;
import org.sonar.plugins.secrets.api.SpecificationConfiguration;
import org.sonar.plugins.secrets.configuration.model.RuleScope;

public abstract class AbstractBinaryFileCheck extends Check {

  // we assume that every binary file check only applies to main files
  private static final List<RuleScope> APPLICABLE_SCOPES = List.of(RuleScope.MAIN);

  protected Predicate<InputFileContext> scopedFilePredicate;

  /**
   * Initialize this check by creating a scope based file predicate.
   *
   * @param specificationConfiguration configuration if test files should be automatically detected
   */
  public void initialize(SpecificationConfiguration specificationConfiguration) {
    this.scopedFilePredicate = ScopeBasedFileFilter.scopeBasedFilePredicate(APPLICABLE_SCOPES, specificationConfiguration);
  }

  @Override
  protected String repositoryKey() {
    return SecretsRulesDefinition.REPOSITORY_KEY;
  }
}
