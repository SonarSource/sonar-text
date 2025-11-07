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
package org.sonar.plugins.secrets;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.event.Level;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonar.check.Rule;
import org.sonar.plugins.common.InputFileContext;
import org.sonar.plugins.common.git.GitService;
import org.sonar.plugins.secrets.api.SpecificationConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;

class AbstractBinaryFileCheckTest {

  @RegisterExtension
  LogTesterJUnit5 logTester = new LogTesterJUnit5().setLevel(Level.DEBUG);

  @Test
  void shouldInitializeScopeFilePredicateAndGitService() {
    var stubCheck = new StubBinaryFileCheck();
    var specificationConfiguration = mock(SpecificationConfiguration.class);
    var gitService = mock(GitService.class);
    stubCheck.initialize(specificationConfiguration, gitService);
    assertThat(stubCheck.scopedFilePredicate).isNotNull();
    assertThat(stubCheck.gitService).isEqualTo(gitService);
  }

  @Test
  void shouldGitServiceNotBeNullBeforeInitialization() {
    var stubCheck = new StubBinaryFileCheck();
    assertThat(stubCheck.gitService).isNotNull();
  }

  @Test
  void shouldReturnUnsuccessfulResultWhenRetrievingUntrackedAndGitServiceNotInitialized() {
    var stubCheck = new StubBinaryFileCheck();
    assertThat(stubCheck.gitService.retrieveUntrackedFileNames()).isEqualTo(GitService.UntrackedFileNamesResult.UNSUCCESSFUL);
    assertThat(logTester.logs(Level.DEBUG))
      .anySatisfy(line -> assertThat(line).contains("Git service has not been initialized, returning unsuccessful result"));
  }

  @Test
  void shouldReturnUnsuccessfulResultWhenRetrievingRepositoryMetadataAndGitServiceNotInitialized() {
    var stubCheck = new StubBinaryFileCheck();
    assertThat(stubCheck.gitService.retrieveRepositoryMetadata()).isEqualTo(GitService.RepositoryMetadataResult.UNSUCCESSFUL);
    assertThat(logTester.logs(Level.DEBUG))
      .anySatisfy(line -> assertThat(line).contains("Git service has not been initialized, returning unsuccessful result"));
  }

  @Test
  void shouldGitServiceCloseWithoutExceptionWhenNotInitialized() {
    var stubCheck = new StubBinaryFileCheck();
    assertThatCode(stubCheck.gitService::close).doesNotThrowAnyException();
  }

  @Rule(key = "StubKey")
  private static class StubBinaryFileCheck extends AbstractBinaryFileCheck {
    @Override
    public void analyze(InputFileContext ctx) {
      // No implementation needed for this test
    }
  }
}
