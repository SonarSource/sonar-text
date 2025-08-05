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
package org.sonar.plugins.secrets.configuration;

import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.scanner.ScannerSide;
import org.sonar.plugins.common.measures.DurationStatistics;
import org.sonar.plugins.secrets.api.SecretsSpecificationLoader;
import org.sonar.plugins.secrets.api.SpecificationLoader;
import org.sonarsource.api.sonarlint.SonarLintSide;

import static org.sonarsource.api.sonarlint.SonarLintSide.INSTANCE;

/**
 * Container for the state of the secrets plugin, that requires one-time initialization.
 * This class improves performance in SonarQube:IDE by avoiding repeated initialization.
 * In the scanner, it acts only as a wrapper for the specification loader.
 */
@ScannerSide
@SonarLintSide(lifespan = INSTANCE)
public class SecretsSpecificationContainer {
  private static final Logger LOG = LoggerFactory.getLogger(SecretsSpecificationContainer.class);

  private SpecificationLoader specificationLoader;

  public SecretsSpecificationContainer() {
    // Default constructor for DI framework
  }

  public void initialize(
    Supplier<SpecificationLoader> constructSpecificationLoader,
    DurationStatistics durationStatistics) {
    if (this.specificationLoader == null) {
      LOG.debug("Initializing SecretsStateContainer with specification loader.");
      this.specificationLoader = durationStatistics.timed("deserializingSpecifications" + DurationStatistics.SUFFIX_GENERAL, constructSpecificationLoader);
    } else {
      LOG.debug("SecretsStateContainer is already initialized, skipping re-initialization.");
    }
  }

  public SecretsSpecificationLoader getSpecificationLoader() {
    if (this.specificationLoader == null) {
      throw new IllegalStateException("Secrets specification loader is not initialized.");
    }
    return (SecretsSpecificationLoader) this.specificationLoader;
  }
}
