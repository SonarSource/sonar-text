/*
 * SonarQube Text Plugin
 * Copyright (C) 2021-2023 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonarsource.text;

import com.sonar.orchestrator.build.SonarScanner;
import java.util.Collection;
import org.assertj.core.util.Arrays;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
public class FileDetectionTest extends TestBase {
  private static final String BASE_DIRECTORY = "projects/file-detection/";
  private static final String TEXT_FILES_ONLY_SUBFOLDER = "text-files-only";
  private static final String MIX_TEXT_BINARY_FILES_SUBFOLDER = "mix-text-binary-files";
  private static final String NO_SONAR_PROFILE_NAME = "nosonar-profile";
  private static final String PROJECT_KEY = "fileDetection";
  private static int counter = 1;

  private String subfolder;
  private boolean analyzeAllMode;
  private String includedTextExtensions;
  private int expectedNumberOfFileAnalyzed;

  public FileDetectionTest(String subfolder, boolean analyzeAllMode, String includedTextExtensions, int expectedNumberOfFileAnalyzed) {
    this.subfolder = subfolder;
    this.analyzeAllMode = analyzeAllMode;
    this.includedTextExtensions = includedTextExtensions;
    this.expectedNumberOfFileAnalyzed = expectedNumberOfFileAnalyzed;
  }

  @Test
  public void shouldFindFilesAsPerConfiguration() {
    // need to define a different project key for every check
    String projectKey = PROJECT_KEY + counter++;

    SonarScanner sonarScanner = getSonarScanner(projectKey, BASE_DIRECTORY + subfolder, NO_SONAR_PROFILE_NAME);
    if (analyzeAllMode) {
      sonarScanner.setProperty("sonar.text.analyzeAllFiles", "true");
    }
    if (includedTextExtensions != null) {
      sonarScanner.setProperty("sonar.text.included.file.suffixes", includedTextExtensions);
    }

    ORCHESTRATOR.executeBuild(sonarScanner);
    if (expectedNumberOfFileAnalyzed > 0) {
      assertThat(getMeasureAsInt(projectKey, "files")).isEqualTo(expectedNumberOfFileAnalyzed);
    } else {
      assertThat(getMeasureAsInt(projectKey, "files")).isNull();
    }
  }

  @Parameterized.Parameters
  public static Collection<Object> configurations() {
    return Arrays.asList(new Object[][] {
      {TEXT_FILES_ONLY_SUBFOLDER, true, null, 2},
      {TEXT_FILES_ONLY_SUBFOLDER, true, "", 2},
      {TEXT_FILES_ONLY_SUBFOLDER, true, " ", 2},
      {TEXT_FILES_ONLY_SUBFOLDER, false, "txt", 1},
      {TEXT_FILES_ONLY_SUBFOLDER, false, null, 0},
      {TEXT_FILES_ONLY_SUBFOLDER, false, "bash,txt", 2},
      {MIX_TEXT_BINARY_FILES_SUBFOLDER, true, null, 2},
      {MIX_TEXT_BINARY_FILES_SUBFOLDER, false, "txt", 1},
      {MIX_TEXT_BINARY_FILES_SUBFOLDER, false, "exe", 1},
      {MIX_TEXT_BINARY_FILES_SUBFOLDER, false, "bash,txt", 2},
    });
  }
}
