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
package org.sonar.plugins.secrets.api;

import java.net.URI;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.plugins.common.InputFileContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

class AutomaticTestFileFilterTest {

  @ParameterizedTest
  @MethodSource("inputsForTestingMainScopeAndSonarTest")
  void shouldAutomaticallyDetectTestFiles(String filePath, boolean shouldMatch) {
    testPredicateWithScopeAndConfiguration(filePath, "/base/directory/", shouldMatch);
  }

  @ParameterizedTest
  @MethodSource("inputsForTestingMainScopeAndSonarTest")
  void shouldAutomaticallyDetectTestFilesWithBaseDirectoryContainingTest(String filePath, boolean shouldMatch) {
    testPredicateWithScopeAndConfiguration(filePath, "/base/directory/with/test/in/it/", shouldMatch);
  }

  private void testPredicateWithScopeAndConfiguration(String filePath, String baseDir, boolean expected) {
    String projectKey = "myProject";

    var inputFile = spy(new TestInputFileBuilder(projectKey, filePath).build());
    URI uri = URI.create("file:" + baseDir + projectKey + "/" + filePath);
    when(inputFile.uri()).thenReturn(uri);

    DefaultFileSystem fileSystem = new DefaultFileSystem(Path.of(baseDir));

    var ctx = mock(InputFileContext.class);
    when(ctx.getInputFile()).thenReturn(inputFile);
    when(ctx.getFileSystem()).thenReturn(fileSystem);

    var predicate = AutomaticTestFileFilter.isNotAutomaticallyDetectedTestFile();

    assertThat(predicate.test(ctx))
      .withFailMessage("Input file uri: " + inputFile.uri().getPath())
      .isEqualTo(expected);
  }

  static Stream<Arguments> inputsForTestingMainScopeAndSonarTest() {
    return Stream.of(
      arguments("testutils.go", false),
      arguments("Testutils.go", false),
      arguments("test_example.py", false),
      arguments("Someutils.go", true),
      arguments("src/main/ExampleFilterTest.java", false),
      arguments("src/main/examplefiltertest.java", false),
      arguments("src/main/ExampleFilterTestt.java", true),
      arguments("docker-compose-api-test.yaml", false),
      arguments("conftest.py", false),
      arguments("example_test.go", false),
      arguments("appsettings.Test.json", false),
      arguments("docker-compose-tests.yaml", false),
      arguments("docker-compose-Tests.yaml", false),
      arguments("docker-compose-Testss.yaml", true),
      arguments("doc/examples/Example.java", false),
      arguments("doc/examples/example.go", false),
      arguments("documentation/examples/example.go", true),
      arguments("examples/docs/example.go", false),
      arguments("examples/docs/example.go", false),
      arguments("examples/docs/src/main/example.go", false),
      arguments("src/test/java/org/sonar/appsettings.json", false),
      arguments("src/Test/java/org/sonar/appsettings.json", false),
      arguments("tests/appsettings.json", false),
      arguments("a/b/c/tests/appsettings.json", false),
      arguments("a/b/c/doc", true), // doc is a filename here
      arguments("libraries/kotlin.test/js/it/js/jest-reporter.js", false),
      arguments("libraries/kotlin.Test/js/it/js/jest-reporter.js", false),
      arguments("native/native.tests/driver/testData/driver0.kt", false),
      arguments("native/native.Tests/driver/testData/driver0.kt", false),
      arguments("latestnews.html", true),
      arguments("abc/latestnews.html", true),
      arguments("contestresults.js", true),
      arguments("Protester.java", true),
      // JS/TS spec files that should be detected as tests
      arguments("component.spec.js", false),
      arguments("Utils.spec.jsx", false),
      arguments("service.spec.ts", false),
      arguments("Component.spec.tsx", false),
      arguments("src/main/component.spec.js", false),
      arguments("feature/user-auth/login.spec.ts", false),
      // Files with "spec" that should NOT be detected as tests
      arguments("specification.js", true),
      arguments("inspect.ts", true),
      arguments("component.spec.py", true),
      arguments("component.spec.java", true),
      arguments("spec-utils.js", true),
      arguments("myspecial.js", true),
      arguments("component.spec.js.md", true),
      // Mocks are very specific for tests
      arguments("mocks/file.json", false),
      arguments("src/app/mocks/hohohoo.txt", false),
      arguments("mock/file.json", false),
      arguments("src/app/mock/merry.txt", false));
  }

  @ParameterizedTest
  @MethodSource("inputsForTestingMainScopeWithWrongBaseDirectory")
  void shouldAutomaticallyDetectTestFilesWhenFileSystemBaseDirPathTypeIsWrong(String filePath, boolean shouldMatch) {
    var inputFile = spy(new TestInputFileBuilder("myProject", filePath).build());
    var uri = URI.create("file:/base/directory/myProject/" + filePath);
    when(inputFile.uri()).thenReturn(uri);

    DefaultFileSystem fileSystem = new DefaultFileSystem(Path.of("./wrong/base/directory/path/type"));

    var ctx = mock(InputFileContext.class);
    when(ctx.getInputFile()).thenReturn(inputFile);
    when(ctx.getFileSystem()).thenReturn(fileSystem);

    var predicate = AutomaticTestFileFilter.isNotAutomaticallyDetectedTestFile();
    assertThat(predicate.test(ctx)).isEqualTo(shouldMatch);
  }

  static Stream<Arguments> inputsForTestingMainScopeWithWrongBaseDirectory() {
    return Stream.of(
      arguments("testutils.go", false),
      arguments("Testutils.go", false),
      arguments("test_example.py", false),
      arguments("Someutils.go", true),
      arguments("src/main/ExampleFilterTest.java", false),
      arguments("src/main/examplefiltertest.java", false),
      arguments("src/main/ExampleFilterTestt.java", true),
      arguments("docker-compose-api-test.yaml", false),
      arguments("conftest.py", false),
      arguments("example_test.go", false),
      arguments("appsettings.Test.json", false),
      arguments("docker-compose-tests.yaml", false),
      arguments("docker-compose-Tests.yaml", false),
      arguments("docker-compose-Testss.yaml", true),
      arguments("doc/examples/Example.java", true),
      arguments("doc/examples/example.go", true),
      arguments("documentation/examples/example.go", true),
      arguments("examples/docs/example.go", true),
      arguments("examples/docs/example.go", true),
      arguments("examples/docs/src/main/example.go", true),
      arguments("src/test/java/org/sonar/appsettings.json", true),
      arguments("src/Test/java/org/sonar/appsettings.json", true),
      arguments("tests/appsettings.json", true),
      arguments("a/b/c/tests/appsettings.json", true),
      arguments("a/b/c/doc", true), // doc is a filename here
      arguments("libraries/kotlin.test/js/it/js/jest-reporter.js", true),
      arguments("libraries/kotlin.Test/js/it/js/jest-reporter.js", true),
      arguments("native/native.tests/driver/testData/driver0.kt", true),
      arguments("native/native.Tests/driver/testData/driver0.kt", true),
      arguments("latestnews.html", true),
      arguments("abc/latestnews.html", true),
      arguments("contestresults.js", true),
      arguments("Protester.java", true),
      // JS/TS spec files that should be detected as tests
      arguments("component.spec.js", false),
      arguments("Utils.spec.jsx", false),
      arguments("service.spec.ts", false),
      arguments("Component.spec.tsx", false),
      arguments("src/main/component.spec.js", false),
      arguments("feature/user-auth/login.spec.ts", false),
      // Files with "spec" that should NOT be detected as tests
      arguments("specification.js", true),
      arguments("inspect.ts", true),
      arguments("component.spec.py", true),
      arguments("component.spec.java", true),
      arguments("spec-utils.js", true),
      arguments("myspecial.js", true),
      arguments("component.spec.js.md", true));
  }
}
