/*
 * SonarQube Text Plugin
 * Copyright (C) 2021-2025 SonarSource Sàrl
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
package org.sonar.plugins.secrets.configuration;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.sonar.plugins.common.measures.DurationStatistics;
import org.sonar.plugins.secrets.api.SecretsSpecificationLoader;
import org.sonar.plugins.secrets.api.SpecificationLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SecretsSpecificationContainerTest {

  @Test
  void shouldInitializeSpecificationLoaderOnlyOnce() {
    var secretsStateContainer = new SecretsSpecificationContainer();
    var durationStatistics = mock(DurationStatistics.class);
    var loader = mock(SecretsSpecificationLoader.class);
    var callCount = new AtomicInteger();
    Supplier<SpecificationLoader> supplier = () -> {
      callCount.incrementAndGet();
      return loader;
    };
    when(durationStatistics.timed(anyString(), Mockito.<Supplier<Object>>any())).thenAnswer(invocation -> {
      Supplier<SpecificationLoader> s = invocation.getArgument(1);
      return s.get();
    });

    secretsStateContainer.initialize(supplier, durationStatistics);
    secretsStateContainer.initialize(supplier, durationStatistics);

    assertThat(callCount.get()).isEqualTo(1);
    assertThat(secretsStateContainer.getSpecificationLoader()).isSameAs(loader);
  }

  @Test
  void shouldReturnSpecificationLoaderOnlyIfInitialized() {
    var secretsStateContainer = new SecretsSpecificationContainer();
    assertThatThrownBy(secretsStateContainer::getSpecificationLoader)
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("Secrets specification loader is not initialized.");

    var durationStatistics = mock(DurationStatistics.class);
    var loader = mock(SecretsSpecificationLoader.class);
    when(durationStatistics.timed(anyString(), Mockito.<Supplier<Object>>any())).thenReturn(loader);

    secretsStateContainer.initialize(() -> loader, durationStatistics);

    assertThat(secretsStateContainer.getSpecificationLoader()).isSameAs(loader);
  }
}
