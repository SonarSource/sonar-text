/*
 * SonarQube Text Plugin
 * Copyright (C) 2021-2026 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.plugins.secrets.utils;

import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonar.check.Rule;
import org.sonar.plugins.common.Check;
import org.sonar.plugins.common.InputFileContext;
import org.sonar.plugins.common.measures.DurationStatistics;
import org.sonar.plugins.secrets.api.SecretsSpecificationLoader;
import org.sonar.plugins.secrets.api.SpecificationBasedCheck;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CheckContainerTest {

  private CheckContainer checkContainer;
  private SecretsSpecificationLoader specificationLoader;
  private DurationStatistics durationStatistics;

  @BeforeEach
  void setUp() {
    checkContainer = new CheckContainer();
    specificationLoader = mock(SecretsSpecificationLoader.class);
    durationStatistics = mock(DurationStatistics.class);
  }

  @Test
  void shouldInitializeSuccessfullyOnFirstCall() {
    var checks = List.of(new StubSpecificationBasedCheck(), new StubNonSpecificationCheck());
    when(durationStatistics.timed(anyString(), any(Supplier.class)))
      .thenAnswer(invocation -> ((Supplier<?>) invocation.getArgument(1)).get());

    checkContainer.initialize(checks, specificationLoader, durationStatistics);

    assertThat(checkContainer.isInitialized()).isTrue();
  }

  @Test
  void shouldSkipReInitializationOnSubsequentCalls() {
    var checks = List.of(new StubSpecificationBasedCheck(), new StubNonSpecificationCheck());
    when(durationStatistics.timed(anyString(), any(Supplier.class)))
      .thenAnswer(invocation -> ((Supplier<?>) invocation.getArgument(1)).get());

    // First initialization
    checkContainer.initialize(checks, specificationLoader, durationStatistics);
    assertThat(checkContainer.isInitialized()).isTrue();

    // Second initialization should be skipped
    checkContainer.initialize(checks, specificationLoader, durationStatistics);

    assertThat(checkContainer.isInitialized()).isTrue();
  }

  @Test
  void shouldThrowExceptionWhenAnalyzingWithoutInitialization() {
    var inputFileContext = mock(InputFileContext.class);

    assertThatThrownBy(() -> checkContainer.analyze(inputFileContext))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("ChecksContainer must be initialized before use. Call initialize() first.");
  }

  @Test
  void shouldThrowExceptionWhenAnalyzingWithRuleIdWithoutInitialization() {
    var inputFileContext = mock(InputFileContext.class);

    assertThatThrownBy(() -> checkContainer.analyze(inputFileContext, "ruleId"))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("ChecksContainer must be initialized before use. Call initialize() first.");
  }

  @Test
  void shouldReturnFalseForIsInitializedBeforeInitialization() {
    assertThat(checkContainer.isInitialized()).isFalse();
  }

  @Rule(key = "exampleSpecificationBasedCheck")
  private static class StubSpecificationBasedCheck extends SpecificationBasedCheck {
    @Override
    public String repositoryKey() {
      return "test-repo";
    }
  }

  @Rule(key = "exampleNonSpecificationBasedCheck")
  private static class StubNonSpecificationCheck extends Check {
    @Override
    public void analyze(InputFileContext inputFileContext) {
      // Stub for tests
    }

    @Override
    public String repositoryKey() {
      return "test-repo";
    }
  }
}
