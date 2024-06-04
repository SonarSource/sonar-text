/*
 * SonarQube Text Plugin
 * Copyright (C) 2021-2024 SonarSource SA
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
package org.sonar.plugins.secrets.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.stream.Stream;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.sonar.plugins.secrets.utils.UpdatingSpecificationFilesGenerator.CHECK_PATH_PREFIX;
import static org.sonar.plugins.secrets.utils.UpdatingSpecificationFilesGenerator.CHECK_TESTS_PATH_PREFIX;
import static org.sonar.plugins.secrets.utils.UpdatingSpecificationFilesGenerator.TEMPLATE_PATH_PREFIX;

class UpdatingSpecificationFilesGeneratorTest {
  private static final UpdatingSpecificationFilesGenerator GENERATOR = new UpdatingSpecificationFilesGenerator();
  private final static String SPECIFICATION_BUILD_PATH = String.join(File.separator, "build", "resources", "test", "org", "sonar",
    "plugins", "secrets", "configuration");
  private final static String SPECIFICATIONS_TO_COPY_PATH = String.join(File.separator, "src", "test", "resources", "secretsConfiguration", "generator");
  private final static Set<String> SPECIFICATION_FILENAMES_TO_GENERATE = Set.of("withoutNewRSPECKey.yaml", "oneNewRSPECKey.yaml",
    "twoNewRSPECKey.yaml");
  private static final Set<String> EXPECTED_GENERATED_CLASSES = Set.of("GeneratorTestOneNewRSPECKeyCheck",
    "GeneratorTestTwoNewRSPECKeyCheck",
    "GeneratorTestTwoNewRSPECKeyUniqueNameCheck");
  private static final Logger LOG = LoggerFactory.getLogger(UpdatingSpecificationFilesGeneratorTest.class);
  @TempDir
  public static File tempFolder;

  /**
   * This is for testing the {@link UpdatingSpecificationFilesGenerator} class. Because the generation process in the class is dependent
   * on that the new secret files exists in the resource folder before the generation process starts, we need to create the files directly
   * in the build folder.
   * Additionally, the generation relies on the correct generation of the <code>SecretsSpecificationFilesDefinition</code> class. 
   * To simulate this we need to add the files manually to the <code>SpecificationLoader</code> class, which is done in 
   * {@link UpdatingSpecificationFilesGenerator#setTestMode(File, Set)}.
   */
  @Test
  void generationShouldWorkAsExpected() throws IOException {
    // arrange
    createSecretsInSpecificationFolder();
    GENERATOR.setTestMode(tempFolder, SPECIFICATION_FILENAMES_TO_GENERATE);

    // act
    GENERATOR.secondStep();

    // assert
    SoftAssertions softly = new SoftAssertions();
    for (String filename : SPECIFICATION_FILENAMES_TO_GENERATE) {
      Path targetPath = Path.of(SPECIFICATION_BUILD_PATH, filename);
      softly.assertThat(targetPath).exists();
    }

    softly.assertThat(Files.readString(Path.of(tempFolder.toPath().toString(), TEMPLATE_PATH_PREFIX, "rspecKeysToUpdate.txt")))
      .isEqualTo("""
        S9000
        S9001
        S9999
        """);

    for (String expectedGeneratedClass : EXPECTED_GENERATED_CLASSES) {
      Path checkPath = Path.of(tempFolder.toPath().toString(), CHECK_PATH_PREFIX, expectedGeneratedClass + ".java");
      Path checkTestPath = Path.of(tempFolder.toPath().toString(), CHECK_TESTS_PATH_PREFIX, expectedGeneratedClass + "Test.java");
      softly.assertThat(checkPath).exists();
      softly.assertThat(checkTestPath).exists();
    }

    try (Stream<Path> files = Files.walk(tempFolder.toPath())) {
      long count = files.filter(Files::isRegularFile).count();
      // verify that no additional files are created
      softly.assertThat(count).isEqualTo(7);
    }

    softly.assertAll();
  }

  private void createSecretsInSpecificationFolder() throws IOException {
    for (String filename : SPECIFICATION_FILENAMES_TO_GENERATE) {
      Path sourcePath = Path.of(SPECIFICATIONS_TO_COPY_PATH, filename);
      Path targetPath = Path.of(SPECIFICATION_BUILD_PATH, filename);

      Files.createDirectories(targetPath.getParent());
      Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
    }
  }

  @AfterAll
  static void cleanUp() {
    for (String filename : SPECIFICATION_FILENAMES_TO_GENERATE) {
      Path targetPath = Path.of(SPECIFICATION_BUILD_PATH, filename);
      deleteFile(targetPath);
    }

    for (String expectedGeneratedClass : EXPECTED_GENERATED_CLASSES) {
      Path checkPath = Path.of(tempFolder.toPath().toString(), CHECK_PATH_PREFIX, expectedGeneratedClass + ".java");
      Path checkTestPath = Path.of(tempFolder.toPath().toString(), CHECK_TESTS_PATH_PREFIX, expectedGeneratedClass + "Test.java");
      deleteFile(checkPath);
      deleteFile(checkTestPath);
    }

    deleteFile(Path.of(tempFolder.toPath().toString(), TEMPLATE_PATH_PREFIX, "rspecKeysToUpdate.txt"));
  }

  private static void deleteFile(Path path) {
    try {
      Files.delete(path);
    } catch (IOException e) {
      // We don't want to fail here, because we want the other files to be deleted
      LOG.warn("Failed to delete file \"{}\"", path, e);
    }
  }
}
