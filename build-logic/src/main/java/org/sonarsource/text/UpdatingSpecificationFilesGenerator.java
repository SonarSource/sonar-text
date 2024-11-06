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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import kotlin.text.StringsKt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Collections.unmodifiableCollection;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toMap;
import static kotlin.text.StringsKt.substringAfter;
import static kotlin.text.StringsKt.substringBefore;
import static org.sonarsource.text.CodeGenerationUtilsKt.listCheckClasses;
import static org.sonarsource.text.CodeGenerationUtilsKt.listSecretSpecificationFiles;

public class UpdatingSpecificationFilesGenerator {

  private static final Logger LOG = LoggerFactory.getLogger(UpdatingSpecificationFilesGenerator.class);
  public static final Path RSPEC_LIST_PATH = Path.of("build/generated");
  private static final Charset CHARSET = StandardCharsets.UTF_8;
  private static final String LINE_SEPARATOR = "\n";
  private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory());
  private static final Pattern FORBIDDEN_SYMBOLS_IN_CHECK_NAME = Pattern.compile("[^\\p{IsAlphabetic}]");

  public final String packagePrefix;
  final Locations locations;

  private final TestingEnvironment testingEnvironment = new TestingEnvironment();
  private final Path projectDir;
  private final Collection<String> keysToExclude;
  private final String baseTestClass;

  record Locations(Path checkTestsPathPrefix, Path checkPathPrefix, Path rspecFilesPath, Path specFilesPathPrefix) {
    public static Locations from(String packagePrefix) {
      var secretsModulePathPrefix = Path.of("src/main/java", packagePrefix, "sonar/plugins/secrets");
      var secretsModuleResourcePathPrefix = Path.of("src/main/resources", packagePrefix, "sonar");

      var checkTestsPathPrefix = Path.of("src/test/java", packagePrefix, "sonar/plugins/secrets/checks");
      var checkPathPrefix = secretsModulePathPrefix.resolve("checks");
      var rspecFilesPath = secretsModuleResourcePathPrefix.resolve("l10n/secrets/rules/secrets");
      var specFilesPathPrefix = secretsModuleResourcePathPrefix.resolve("plugins/secrets/configuration");
      return new Locations(checkTestsPathPrefix, checkPathPrefix, rspecFilesPath, specFilesPathPrefix);
    }
  }

  public UpdatingSpecificationFilesGenerator(String projectDir, String packagePrefix, Collection<String> keysToExclude, String baseTestClass) {
    this.packagePrefix = packagePrefix;
    this.baseTestClass = baseTestClass;
    this.projectDir = Path.of(projectDir);
    this.keysToExclude = unmodifiableCollection(keysToExclude);
    this.locations = Locations.from(packagePrefix);
  }

  /**
   * Performs generation for configuration files and checks.
   * <ol>
   * <li>Determines keys that need to be implemented and keys that are not used anymore.</li>
   * <li>Generates Java classes and tests for new keys.</li>
   * <li>Removes Java classes and tests for keys that are not used anymore.</li>
   * <li>Creates a file `rspecKeysToUpdate.txt` to be processed and provided into the Rules API later.</li>
   * </ol>
   */
  public void performGeneration() {
    var specificationsToLoad = new HashSet<>(listSecretSpecificationFiles(projectDir.resolve(locations.specFilesPathPrefix)));
    if (testingEnvironment.testingEnabled) {
      specificationsToLoad.addAll(testingEnvironment.additionalSpecificationFiles(projectDir));
    }
    LOG.debug("Found specifications: {}", specificationsToLoad);

    var rulesMappedToKey = readAllRulesFromSpecifications(specificationsToLoad)
      .collect(toMap(spec -> spec.get("rspecKey").asText(), Function.identity(), (rule1, rule2) -> rule1));
    var checkNamesMappedToKey = readAllSpecifications(specificationsToLoad)
      .flatMap(spec -> rulesStream(spec).map(rule -> Map.entry(rule.get("rspecKey").asText(), providerName(spec))))
      .collect(toMap(Map.Entry::getKey, Map.Entry::getValue, (name1, name2) -> name1));

    Map<String, String> existingKeysMappedToFileName = retrieveAlreadyExistingKeys(projectDir);

    keysToExclude.forEach((String key) -> {
      rulesMappedToKey.remove(key);
      existingKeysMappedToFileName.remove(key);
    });

    Set<String> keysToImplementChecksFor = new HashSet<>(rulesMappedToKey.keySet());
    keysToImplementChecksFor.removeAll(existingKeysMappedToFileName.keySet());

    Set<String> keysNotUsedAnymore = new HashSet<>(existingKeysMappedToFileName.keySet());
    keysNotUsedAnymore.removeAll(rulesMappedToKey.keySet());

    LOG.debug("All keys: {}, existing keys: {}, keys to implement checks for: {}, keys not used anymore: {}", rulesMappedToKey.keySet(),
      existingKeysMappedToFileName.keySet(), keysToImplementChecksFor, keysNotUsedAnymore);

    for (String rspecKey : keysToImplementChecksFor) {
      var checkName = checkNamesMappedToKey.get(rspecKey);
      checkName = sanitizeCheckName(checkName, rspecKey, existingKeysMappedToFileName);
      writeCheckFile(checkName, rspecKey, "/templates/GenericCheckTemplate.java");
      writeCheckTestFile(checkName, "/templates/GenericCheckTestTemplate.java");
    }

    removeUnusedChecks(keysNotUsedAnymore, existingKeysMappedToFileName);
    constructFileForRulesAPI(keysToImplementChecksFor);
  }

  /**
   * Retrieve keys for already implemented rules
   * @return mapping ruleKey -> checkClassName
   */
  Map<String, String> retrieveAlreadyExistingKeys(Path projectDir) {
    var checkClasses = listCheckClasses(projectDir.resolve(locations.checkPathPrefix));

    return checkClasses.stream()
      .map(file -> Map.entry(readRuleKey(file), file.getName().replace(".java", "")))
      .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  static String readRuleKey(File file) {
    try {
      var ruleAnnotation = Files.readAllLines(file.toPath(), CHARSET).stream()
        .filter(s -> s.contains("Rule(key = \""))
        .findFirst()
        .orElseThrow(() -> new RuntimeException("No Rule annotation found in file " + file.getName()));
      var rspecKey = substringAfter(ruleAnnotation, "key = \"", "");
      rspecKey = substringBefore(rspecKey, "\"", "");
      return rspecKey;
    } catch (IOException e) {
      throw new GenerationException("unable to extract rule key from file" + file.getName(), e);
    }
  }

  static String sanitizeCheckName(String checkName, String rspecKey, Map<String, String> existingClassNames) {
    checkName = FORBIDDEN_SYMBOLS_IN_CHECK_NAME.matcher(checkName).replaceAll("") + "Check";

    while (existingClassNames.containsValue(checkName)) {
      checkName = checkName.replace("Check", "UniqueNameCheck");
    }
    existingClassNames.put(rspecKey, checkName);
    return checkName;
  }

  void writeCheckFile(String checkName, String rspecKey, String resourcePath) {
    var checkPath = locations.checkPathPrefix.resolve(checkName + ".java");
    try (var stream = UpdatingSpecificationFilesGenerator.class.getResourceAsStream(resourcePath)) {
      requireNonNull(stream, "File GenericCheckTemplate not found");
      var content = new String(stream.readAllBytes(), CHARSET)
        .replace("<package>", packagePrefix + ".sonar.plugins.secrets.checks")
        .replace("GenericCheckTemplate", checkName)
        .replace("<RSPEC-KEY>", rspecKey);
      writeFile(checkPath, content);
    } catch (IOException | NullPointerException e) {
      throw new GenerationException("error while writing check file for the check " + checkName, e);
    }

    LOG.info("Successfully generated Check \"{}.java\" with rspecKey {}", checkName, rspecKey);
  }

  void writeCheckTestFile(String checkName, String resourcePath) {
    var checkTestPath = locations.checkTestsPathPrefix.resolve(checkName + "Test.java");
    try (var stream = UpdatingSpecificationFilesGenerator.class.getResourceAsStream(resourcePath)) {
      requireNonNull(stream, "File GenericCheckTestTemplate not found");
      var baseTestClassName = StringsKt.substringAfterLast(baseTestClass, ".", "");
      var content = new String(stream.readAllBytes(), CHARSET)
        .replace("<package>", packagePrefix + ".sonar.plugins.secrets.checks")
        .replace("<base-test-class>", baseTestClass)
        .replace("<base-test-class-name>", baseTestClassName)
        .replace("GenericCheckTemplate", checkName)
        .replace("GenericCheckTemplateTest", checkName + "Test");
      writeFile(checkTestPath, content);
    } catch (IOException | NullPointerException e) {
      throw new GenerationException("error while writing check test file for the check " + checkName, e);
    }

    LOG.info("Successfully generated Check \"{}Test.java\"", checkName);
  }

  private void removeUnusedChecks(Set<String> keysNotUsedAnymore, Map<String, String> rspecKeysMappedToCheckNames) {
    for (String key : keysNotUsedAnymore) {
      String filename = rspecKeysMappedToCheckNames.get(key);

      if (filename != null) {
        removeUnusedCheck(filename, key);
      }
    }
  }

  private void removeUnusedCheck(String checkName, String rspecKey) {
    var checkPath = projectDir.resolve(locations.checkPathPrefix).resolve(checkName + ".java");
    var checkTestPath = projectDir.resolve(locations.checkTestsPathPrefix).resolve(checkName + "Test.java");
    var rspecJson = projectDir.resolve(locations.rspecFilesPath).resolve(rspecKey + ".json");
    var rspecHtml = projectDir.resolve(locations.rspecFilesPath).resolve(rspecKey + ".html");

    try {
      Files.deleteIfExists(checkPath);
      Files.deleteIfExists(checkTestPath);
      Files.deleteIfExists(rspecJson);
      Files.deleteIfExists(rspecHtml);
    } catch (IOException e) {
      throw new GenerationException("error while deleting Check with name " + checkName, e);
    }

    LOG.info("Successfully removed Check \"{}\" with rspecKey %s", checkName);
  }

  private void constructFileForRulesAPI(Set<String> keysToUpdateRuleAPIFor) {
    var pathToWriteUpdateFileTo = projectDir.resolve("build/generated/rspecKeysToUpdate.txt");
    String content = generateContentForRulesAPIUpdateFile(keysToUpdateRuleAPIFor);
    writeFile(pathToWriteUpdateFileTo, content);
    LOG.info("Successfully generated rule-api update file");
  }

  private static String generateContentForRulesAPIUpdateFile(Set<String> keysToUpdateRuleAPIFor) {
    var sb = new StringBuilder();
    for (String key : keysToUpdateRuleAPIFor) {
      sb.append(key);
      sb.append(LINE_SEPARATOR);
    }
    return sb.toString();
  }

  void writeFile(Path path, String content) {
    try {
      if (testingEnvironment.testingEnabled) {
        path = Path.of(testingEnvironment.outputFolder.getAbsolutePath(), path.toString());
        Files.createDirectories(path.getParent());
      }
      var fullPath = projectDir.resolve(path);
      LOG.debug("Writing file {}", fullPath);
      Files.writeString(fullPath, content, CHARSET);
    } catch (IOException e) {
      throw new GenerationException("error while writing file " + path, e);
    }
  }

  public void setTestMode(File outputFolder, Set<String> additionalSpecificationFilenames) {
    testingEnvironment.testingEnabled = true;
    testingEnvironment.outputFolder = outputFolder;
    testingEnvironment.additionalSpecificationFilenames.addAll(additionalSpecificationFilenames);
  }

  static Stream<JsonNode> readAllRulesFromSpecifications(Collection<File> specificationFiles) {
    return readAllSpecifications(specificationFiles)
      .flatMap(UpdatingSpecificationFilesGenerator::rulesStream);
  }

  static Stream<JsonNode> readAllSpecifications(Collection<File> specificationFiles) {
    return specificationFiles.stream()
      .map((File file) -> {
        try {
          return MAPPER.readValue(file, JsonNode.class);
        } catch (IOException e) {
          throw new GenerationException("error while reading specification file " + file, e);
        }
      });
  }

  private static Stream<JsonNode> rulesStream(JsonNode spec) {
    return StreamSupport.stream(spec.get("provider").get("rules").spliterator(), false);
  }

  private static String providerName(JsonNode spec) {
    return spec.get("provider").get("metadata").get("name").asText();
  }

  static class TestingEnvironment {
    public static final String SPEC_FILES_LOCATION = "src/test/resources/secretsConfiguration/generator/";

    private boolean testingEnabled;
    private File outputFolder;
    private final Set<String> additionalSpecificationFilenames = new HashSet<>();

    public TestingEnvironment() {
      this.testingEnabled = false;
      this.outputFolder = null;
    }

    public Collection<File> additionalSpecificationFiles(Path baseDir) {
      return additionalSpecificationFilenames.stream()
        .map(fileName -> baseDir.resolve(TestingEnvironment.SPEC_FILES_LOCATION + fileName).toFile())
        .collect(Collectors.toSet());
    }
  }
}
