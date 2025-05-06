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
package org.sonar.plugins.common.git;

import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CachingGitServiceTest {

  private GitService underlyingService;
  private GitService cachingGitService;

  @BeforeEach
  void setUp() {
    underlyingService = mock(GitService.class);
    cachingGitService = new CachingGitService(underlyingService);
  }

  @Test
  void shouldRetrieveUntrackedFileNamesOnlyOnce() {
    var expectedResult = new GitService.UntrackedFileNamesResult(true, Set.of("file1.txt"));
    when(underlyingService.retrieveUntrackedFileNames()).thenReturn(expectedResult);

    var result1 = cachingGitService.retrieveUntrackedFileNames();
    var result2 = cachingGitService.retrieveUntrackedFileNames();

    assertThat(result1).isSameAs(result2).isEqualTo(expectedResult);
    verify(underlyingService, times(1)).retrieveUntrackedFileNames();
  }

  @Test
  void shouldRetrieveRepositoryMetadataOnlyOnce() {
    var expectedResult = new GitService.RepositoryMetadataResult(true, "project", "org");
    when(underlyingService.retrieveRepositoryMetadata()).thenReturn(expectedResult);

    var result1 = cachingGitService.retrieveRepositoryMetadata();
    var result2 = cachingGitService.retrieveRepositoryMetadata();

    assertThat(result1).isSameAs(result2).isEqualTo(expectedResult);
    verify(underlyingService, times(1)).retrieveRepositoryMetadata();
  }

  @Test
  void shouldCloseUnderlyingService() throws Exception {
    cachingGitService.close();
    verify(underlyingService, times(1)).close();
  }

  @Test
  void shouldBeThreadSafeWhenRetrievingUntrackedFileNames() throws InterruptedException {
    when(underlyingService.retrieveUntrackedFileNames()).thenReturn(new GitService.UntrackedFileNamesResult(true, Set.of("file1.txt")));
    runConcurrently(cachingGitService::retrieveUntrackedFileNames);
    verify(underlyingService, times(1)).retrieveUntrackedFileNames();
  }

  @Test
  void shouldBeThreadSafeWhenRetrievingRepositoryMetadata() throws InterruptedException {
    when(underlyingService.retrieveRepositoryMetadata()).thenReturn(new GitService.RepositoryMetadataResult(true, "project", "org"));
    runConcurrently(cachingGitService::retrieveRepositoryMetadata);
    verify(underlyingService, times(1)).retrieveRepositoryMetadata();
  }

  private void runConcurrently(Runnable task) throws InterruptedException {
    var threadCount = 10;
    var executor = Executors.newFixedThreadPool(threadCount);
    var latch = new CountDownLatch(threadCount);

    for (int i = 0; i < threadCount; i++) {
      executor.submit(() -> {
        task.run();
        latch.countDown();
      });
    }

    latch.await();
    executor.shutdown();
  }
}
