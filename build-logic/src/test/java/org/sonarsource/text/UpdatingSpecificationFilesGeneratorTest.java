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
package org.sonarsource.text;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.stream.Stream;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonarsource.text.UpdatingSpecificationFilesGenerator.CHECK_PATH_PREFIX;
import static org.sonarsource.text.UpdatingSpecificationFilesGenerator.CHECK_TESTS_PATH_PREFIX;
import static org.sonarsource.text.UpdatingSpecificationFilesGenerator.RSPEC_LIST_PATH;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UpdatingSpecificationFilesGeneratorTest {
  private static final UpdatingSpecificationFilesGenerator GENERATOR = new UpdatingSpecificationFilesGenerator(".");
  private static final String SPECIFICATION_BUILD_PATH = String.join(File.separator, "build", "resources", "test", "org", "sonar",
    "plugins", "secrets", "configuration");
  private static final String SPECIFICATIONS_TO_COPY_PATH = String.join(File.separator, "src", "test", "resources", "secretsConfiguration", "generator");
  private static final Set<String> SPECIFICATION_FILENAMES_TO_GENERATE = Set.of("withoutNewRSPECKey.yaml", "oneNewRSPECKey.yaml",
    "twoNewRSPECKey.yaml");
  private static final Set<String> EXPECTED_GENERATED_CLASSES = Set.of("GeneratorTestOneNewRSPECKeyCheck",
    "GeneratorTestTwoNewRSPECKeyCheck",
    "GeneratorTestTwoNewRSPECKeyUniqueNameCheck");
  private static final Logger LOG = LoggerFactory.getLogger(UpdatingSpecificationFilesGeneratorTest.class);
  @TempDir
  public static File tempFolder;

  @BeforeAll
  static void setUp() throws IOException {
    Files.createDirectories(Path.of(CHECK_PATH_PREFIX));

    // Create a mock check class to test the logic for existing checks
    String checkClass = """
      package org.sonar.plugins.secrets.checks;
      import org.sonar.check.Rule;
      @Rule(key = "S6733")
      public class ExistingCheck {
      }
      """;
    Files.writeString(Path.of(CHECK_PATH_PREFIX, "ExistingCheck.java"), checkClass);

    String unusedCheckClass = """
      package org.sonar.plugins.secrets.checks;
      import org.sonar.check.Rule;
      @Rule(key = "S0000")
      public class UnusedCheck {
      }
      """;
    Files.writeString(Path.of(CHECK_PATH_PREFIX, "UnusedCheck.java"), unusedCheckClass);
  }

  @Test
  void shouldRetrieveExistingChecks() {
    var existingChecksMappedToKey = UpdatingSpecificationFilesGenerator.retrieveAlreadyExistingKeys(".");

    assertThat(existingChecksMappedToKey).containsOnlyKeys("S6733", "S0000");
  }

  @Test
  void shouldThrowIfCheckFileCannotBeRead() {
    assertThatThrownBy(() -> UpdatingSpecificationFilesGenerator.readRuleKey(new File("nonExistentFile")))
      .isInstanceOf(GenerationException.class)
      .hasCauseInstanceOf(IOException.class);
  }

  @Test
  void shouldThrowIfCheckTemplateCannotBeRead() {
    var generator = new UpdatingSpecificationFilesGenerator(".");
    assertThatThrownBy(() -> generator.writeCheckFile("FooCheck.java", "S0000", "nonExistentFile"))
      .isInstanceOf(GenerationException.class)
      .hasCauseInstanceOf(NullPointerException.class);
  }

  @Test
  void shouldThrowIfCheckTestTemplateCannotBeRead() {
    var generator = new UpdatingSpecificationFilesGenerator(".");
    assertThatThrownBy(() -> generator.writeCheckTestFile("FooCheckTest.java", "nonExistentFile"))
      .isInstanceOf(GenerationException.class)
      .hasCauseInstanceOf(NullPointerException.class);
  }

  @Test
  void shouldThrowIfFileCannotBeWritten() {
    var generator = new UpdatingSpecificationFilesGenerator(".");
    assertThatThrownBy(() -> generator.writeFile(Path.of("directory/that/does/not/exist"), "foobar"))
      .isInstanceOf(GenerationException.class)
      .hasCauseInstanceOf(IOException.class);
  }

  @Test
  void shouldThrowIfSpecificationFileCannotBeRead() {
      var generator = new UpdatingSpecificationFilesGenerator(".");
      generator.setTestMode(tempFolder, Set.of("nonExistentFile.yaml"));

      assertThatThrownBy(() -> generator.readAllSpecifications().toList())
        .isInstanceOf(GenerationException.class)
        .hasCauseInstanceOf(IOException.class);
  }

  /**
   * This is for testing the {@link UpdatingSpecificationFilesGenerator} class. Because the generation process in the class is dependent
   * on that the new secret files exists in the resource folder before the generation process starts, we need to create the files directly
   * in the build folder.
   * Additionally, the generation relies on the correct generation of the <code>SecretsSpecificationFilesDefinition</code> class. 
   * To simulate this we need to add the files manually to the <code>SpecificationLoader</code> class, which is done in 
   * {@link UpdatingSpecificationFilesGenerator#setTestMode(File, Set)}.
   */
  @Test
  @Order(Integer.MAX_VALUE)
  void generationShouldWorkAsExpected() throws IOException {
    // arrange
    createSecretsInSpecificationFolder();
    GENERATOR.setTestMode(tempFolder, SPECIFICATION_FILENAMES_TO_GENERATE);

    // act
    GENERATOR.performGeneration();

    // assert
    SoftAssertions softly = new SoftAssertions();
    for (String filename : SPECIFICATION_FILENAMES_TO_GENERATE) {
      Path targetPath = Path.of(SPECIFICATION_BUILD_PATH, filename);
      softly.assertThat(targetPath).exists();
    }

    softly.assertThat(Files.readString(tempFolder.toPath().resolve(RSPEC_LIST_PATH).resolve("rspecKeysToUpdate.txt")))
      .isEqualTo("""
        S9000
        S9001
        S9999
        """);

    for (String expectedGeneratedClass : EXPECTED_GENERATED_CLASSES) {
      Path checkPath = tempFolder.toPath().resolve(CHECK_PATH_PREFIX).resolve(expectedGeneratedClass + ".java");
      Path checkTestPath = tempFolder.toPath().resolve(CHECK_TESTS_PATH_PREFIX).resolve(expectedGeneratedClass + "Test.java");
      softly.assertThat(checkPath).exists();
      softly.assertThat(checkTestPath).exists();
    }
    softly.assertThat(tempFolder.toPath().resolve(CHECK_PATH_PREFIX).resolve("UnusedCheck.java"))
      .as("Unused check class should have been deleted")
      .doesNotExist();

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
      Path checkPath = tempFolder.toPath().resolve(CHECK_PATH_PREFIX).resolve(expectedGeneratedClass + ".java");
      Path checkTestPath = tempFolder.toPath().resolve(CHECK_TESTS_PATH_PREFIX).resolve(expectedGeneratedClass + "Test.java");
      deleteFile(checkPath);
      deleteFile(checkTestPath);
    }

    deleteFile(tempFolder.toPath().resolve(RSPEC_LIST_PATH).resolve("rspecKeysToUpdate.txt"));

    deleteFile(Path.of(CHECK_PATH_PREFIX, "ExistingCheck.java"));
    deleteFile(Path.of(CHECK_PATH_PREFIX, "UnusedCheck.java"));
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
