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
package org.sonar.plugins.text.checks;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.plugins.common.Check;
import org.sonar.plugins.common.InputFileContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.plugins.common.TestUtils.analyze;
import static org.sonar.plugins.common.TestUtils.inputFileFromPath;
import static org.sonar.plugins.common.TestUtils.mockDurationStatistics;
import static org.sonar.plugins.text.checks.BIDICharacterCheck.isAndroidI18nFile;

class BIDICharacterCheckTest {

  @Test
  void shouldCheckFile() throws IOException {
    var check = getCheck();
    var file = inputFileFromPath(Path.of("src", "test", "resources", "checks", "BIDICharacterCheck", "test.php"));
    assertThat(analyze(check, file)).containsExactly(
      "text:S6389 [3:0-3:20] This line contains a bidirectional character in column 12. Make sure that using bidirectional characters is safe here.",
      "text:S6389 [4:0-4:20] This line contains a bidirectional character in column 12. Make sure that using bidirectional characters is safe here.",
      "text:S6389 [5:0-5:20] This line contains a bidirectional character in column 12. Make sure that using bidirectional characters is safe here.",
      "text:S6389 [6:0-6:20] This line contains a bidirectional character in column 12. Make sure that using bidirectional characters is safe here.",
      "text:S6389 [7:0-7:20] This line contains a bidirectional character in column 12. Make sure that using bidirectional characters is safe here.",
      "text:S6389 [8:0-8:20] This line contains a bidirectional character in column 12. Make sure that using bidirectional characters is safe here.",
      "text:S6389 [9:0-9:20] This line contains a bidirectional character in column 12. Make sure that using bidirectional characters is safe here.",
      "text:S6389 [12:0-12:30] This line contains a bidirectional character in column 12. Make sure that using bidirectional characters is safe here.",
      "text:S6389 [13:0-13:30] This line contains a bidirectional character in column 12. Make sure that using bidirectional characters is safe here.",
      "text:S6389 [16:0-16:40] This line contains a bidirectional character in column 32. Make sure that using bidirectional characters is safe here.",
      "text:S6389 [17:0-17:40] This line contains a bidirectional character in column 32. Make sure that using bidirectional characters is safe here.",
      "text:S6389 [20:0-20:30] This line contains a bidirectional character in column 12. Make sure that using bidirectional characters is safe here.",
      "text:S6389 [21:0-21:30] This line contains a bidirectional character in column 12. Make sure that using bidirectional characters is safe here.");
  }

  @Test
  void shouldNotAnalyzeAndroidI18nFiles() throws IOException {
    var check = getCheck();
    var file = inputFileFromPath(Path.of("src", "test", "resources", "checks", "BIDICharacterCheck", "res", "values-pl", "strings.xml"));
    assertThat(analyze(check, file)).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(strings = {
    "file:///src/main/res/values-ka-rGE/strings.xml",
    "notfile:///src/main/res/values-ka-rGE/strings.xml",
    "src/main/res/values-ka-rGE/strings.xml",
    "app/src/main/res/values-en-rUS/strings.xml",
    "source/app/src/main/res/values-fa/strings.xml",
    "source/app/src/main/res/values--/strings.xml",
    "res/values-fa/strings.xml",
    "/res/values-fa/strings.xml"
  })
  void shouldIgnoreAndroidI18nFiles(String uri) throws URISyntaxException {
    var ctx = prepareInputFileContext(uri);

    assertThat(isAndroidI18nFile(ctx)).isTrue();
  }

  @ParameterizedTest
  @ValueSource(strings = {
    "file:///src/main/res/values-/strings.xml",
    "notfile:///src/main/res/values-/strings.xml",
    "src/main/res/values/strings.xml",
    "src/main/res/values-/strings.xml",
    "src/main/res/values-ka-rGE/colors.xml",
    "src/main/java/com/example/Main.java",
    "/src/main/java/com/example/Main.java",
    "resources/values-fr/strings.xml",
    "values-fr/strings.xml",
    "values-fr/strings.xml/foo.bar",
    "strings.xml",
    "/strings.xml",
  })
  void shouldNotIgnoreAndroidI18nFiles(String uri) throws URISyntaxException {
    var ctx = prepareInputFileContext(uri);

    assertThat(isAndroidI18nFile(ctx)).isFalse();
  }

  private static Check getCheck() {
    var check = new BIDICharacterCheck();
    check.initialize(mockDurationStatistics());
    return check;
  }

  private static InputFileContext prepareInputFileContext(String uri) throws URISyntaxException {
    var ctx = mock(InputFileContext.class);
    var inputFileMock = mock(InputFile.class);
    when(inputFileMock.filename()).thenReturn(uri.substring(uri.lastIndexOf('/') + 1));
    when(inputFileMock.uri()).thenReturn(new URI(uri));
    when(ctx.getInputFile()).thenReturn(inputFileMock);
    return ctx;
  }
}
