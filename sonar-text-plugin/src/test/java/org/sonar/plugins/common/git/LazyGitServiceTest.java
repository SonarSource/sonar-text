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
package org.sonar.plugins.common.git;

import java.util.Set;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class LazyGitServiceTest {

  private GitService mockService;
  private Supplier<GitService> mockSupplier;
  private LazyGitService lazyGitService;

  @BeforeEach
  void setUp() {
    mockService = mock(GitService.class);
    mockSupplier = mock(Supplier.class);
    when(mockSupplier.get()).thenReturn(mockService);
    lazyGitService = new LazyGitService(mockSupplier);
  }

  @Test
  void shouldInitializeServiceLazilyWhenRetrievingUntracked() {
    verifyNoInteractions(mockSupplier);
    lazyGitService.retrieveUntrackedFileNames();
    verify(mockSupplier, times(1)).get();
  }

  @Test
  void shouldInitializeServiceLazilyWhenRetrievingRepositoryMetadata() {
    verifyNoInteractions(mockSupplier);
    lazyGitService.retrieveRepositoryMetadata();
    verify(mockSupplier, times(1)).get();
  }

  @Test
  void shouldDelegateRetrieveUntrackedFileNames() {
    var expectedResult = new GitService.UntrackedFileNamesResult(true, Set.of("file1.txt"));
    when(mockService.retrieveUntrackedFileNames()).thenReturn(expectedResult);

    var result = lazyGitService.retrieveUntrackedFileNames();

    assertThat(result).isEqualTo(expectedResult);
    verify(mockService, times(1)).retrieveUntrackedFileNames();
  }

  @Test
  void shouldDelegateRetrieveRepositoryMetadata() {
    var expectedResult = new GitService.RepositoryMetadataResult(true, "project", "org");
    when(mockService.retrieveRepositoryMetadata()).thenReturn(expectedResult);

    var result = lazyGitService.retrieveRepositoryMetadata();

    assertThat(result).isEqualTo(expectedResult);
    verify(mockService, times(1)).retrieveRepositoryMetadata();
  }

  @Test
  void shouldCallCloseOnUnderlyingServiceWhenClosingAfterUsing() throws Exception {
    lazyGitService.retrieveUntrackedFileNames();
    lazyGitService.close();
    verify(mockService, times(1)).close();
  }

  @Test
  void shouldNotInitializeServiceWhenClosingWithoutUsing() throws Exception {
    lazyGitService.close();
    verify(mockSupplier, never()).get();
  }

  @Test
  void shouldReuseServiceInstance() {
    lazyGitService.retrieveUntrackedFileNames();
    lazyGitService.retrieveRepositoryMetadata();
    verify(mockSupplier, times(1)).get();
  }
}
