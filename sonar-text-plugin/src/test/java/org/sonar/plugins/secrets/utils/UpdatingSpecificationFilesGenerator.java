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
import java.lang.reflect.Constructor;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.sonar.api.internal.apachecommons.io.FileUtils;
import org.sonar.api.testfixtures.log.LogAndArguments;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonar.plugins.common.Check;
import org.sonar.plugins.secrets.SecretsCheckList;
import org.sonar.plugins.secrets.SecretsRulesDefinition;
import org.sonar.plugins.secrets.api.SpecificationLoader;
import org.sonar.plugins.secrets.configuration.model.Rule;

// This name is intentionally not ending in "Test" to not get picked up automatically by maven
@SuppressWarnings("java:S3577")
class UpdatingSpecificationFilesGenerator {

  private final static String CHECK_TESTS_PATH_PREFIX = String.join(File.separator, "src", "test", "java", "org", "sonar", "plugins", "secrets", "checks");
  private final static String SECRETS_MODULE_PATH_PREFIX = String.join(File.separator, "src", "main", "java", "org", "sonar", "plugins", "secrets");
  private final static String SECRETS_MODULE_RESOURCE_PATH_PREFIX = String.join(File.separator, "src", "main", "resources", "org", "sonar");
  private final static String SPECIFICATION_FILES_PATH = String.join(File.separator, SECRETS_MODULE_RESOURCE_PATH_PREFIX, "plugins", "secrets");
  private final static String CHECK_PATH_PREFIX = String.join(File.separator, SECRETS_MODULE_PATH_PREFIX, "checks");
  private final static String TEMPLATE_PATH_PREFIX = String.join(File.separator, "src", "test", "resources", "templates");
  private final static String RSPEC_FILES_PATH_PREFIX = String.join(File.separator, SECRETS_MODULE_RESOURCE_PATH_PREFIX, "l10n", "secrets", "rules", "secrets");
  private final Charset charset = StandardCharsets.UTF_8;
  private static final Logger LOG = LoggerFactory.getLogger(UpdatingSpecificationFilesGenerator.class);

  @RegisterExtension
  LogTesterJUnit5 logTester = new LogTesterJUnit5();

  // Suppress warning, as there are no assertions inside here
  @Test
  @Disabled("Should only be triggered manually")
  @SuppressWarnings("java:S2699")
  void firstStep() {
    writeSpecificationFileDefinition();
  }

  @Test
  @Disabled("Should only be triggered manually")
  void secondStep() {
    testDeserializationOfSpecificationFiles();
    SpecificationLoader specificationLoader = new SpecificationLoader();

    Map<String, String> existingKeysMappedToFileName = retrieveAlreadyExistingKeys();

    Map<String, List<Rule>> rulesMappedToKey = specificationLoader.getRulesMappedToKey();

    Set<String> keysToImplementChecksFor = new HashSet<>(rulesMappedToKey.keySet());
    keysToImplementChecksFor.removeAll(existingKeysMappedToFileName.keySet());

    Set<String> keysNotUsedAnymore = new HashSet<>(existingKeysMappedToFileName.keySet());
    keysNotUsedAnymore.removeAll(rulesMappedToKey.keySet());

    List<String> newCheckNames = new ArrayList<>();
    for (String rspecKey : keysToImplementChecksFor) {
      String checkName = rulesMappedToKey.get(rspecKey).get(0).getProvider().getMetadata().getName();
      checkName = sanitizeCheckName(checkName, rspecKey, existingKeysMappedToFileName);
      writeCheckFile(checkName, rspecKey);
      writeCheckTestFile(checkName);
      newCheckNames.add(checkName);
    }

    Set<String> checkNamesNotUsedAnymore = keysNotUsedAnymore.stream().map(existingKeysMappedToFileName::get).collect(Collectors.toSet());

    removeUnusedChecks(keysNotUsedAnymore, existingKeysMappedToFileName);
    writeUpdatedRulesDefinition(newCheckNames, checkNamesNotUsedAnymore);
    constructFileForRulesAPI(keysToImplementChecksFor);
  }

  private Map<String, String> retrieveAlreadyExistingKeys() {
    Map<String, String> keyToFileName = new HashMap<>();

    List<Class<?>> checks = new SecretsCheckList().checks();
    for (Class<?> check : checks) {
      Constructor<?>[] declaredConstructors = check.getDeclaredConstructors();
      Check instantiatedCheck;
      try {
        instantiatedCheck = (Check) declaredConstructors[0].newInstance();
      } catch (Exception e) {
        LOG.error("Error while retrieving already existing keys");
        throw new RuntimeException(e);
      }
      keyToFileName.put(instantiatedCheck.getRuleKey().rule(), check.getSimpleName());
    }

    return keyToFileName;
  }

  private String sanitizeCheckName(String checkName, String rspecKey, Map<String, String> existingClassNames) {
    checkName = checkName.replaceAll("[^a-zA-Z]", "");
    checkName = checkName + "Check";

    if (existingClassNames.containsValue(checkName)) {
      checkName = checkName.replace("Check", "UniqueNameCheck");
    }
    existingClassNames.put(rspecKey, checkName);
    return checkName;
  }

  private void writeCheckFile(String checkName, String rspecKey) {
    Path checkPath = Path.of(CHECK_PATH_PREFIX, checkName + ".java");
    Path checkTemplatePath = Path.of(TEMPLATE_PATH_PREFIX, "GenericCheckTemplate.java");
    try {
      Files.copy(checkTemplatePath, checkPath);

      String content = Files.readString(checkPath, charset);
      content = content.replace("GenericCheckTemplate", checkName);
      content = content.replace("<RSPEC-KEY>", rspecKey);
      Files.write(checkPath, content.getBytes(charset));
    } catch (IOException e) {
      LOG.error("Error while writing Check file with name \"" + checkName + ".java\", please fix manually", e);
      throw new RuntimeException(e);
    }

    LOG.info("Successfully generated Check \"{}.java\" with rspecKey {}", checkName, rspecKey);
  }

  private void writeCheckTestFile(String checkName) {
    Path checkTestPath = Path.of(CHECK_TESTS_PATH_PREFIX, checkName + "Test.java");
    Path checkTestTemplatePath = Path.of(TEMPLATE_PATH_PREFIX, "GenericCheckTestTemplate.java");
    try {
      Files.copy(checkTestTemplatePath, checkTestPath);

      String content = Files.readString(checkTestPath, charset);
      content = content.replace("GenericCheckTemplate", checkName);
      content = content.replace("GenericCheckTemplateTest", checkName + "Test");
      Files.write(checkTestPath, content.getBytes(charset));
    } catch (IOException e) {
      LOG.error("Error while writing Check Test file with name \"" + checkName + "Test.java\", please fix manually", e);
      throw new RuntimeException(e);
    }

    LOG.info("Successfully generated Check \"{}Test.java\"", checkName);
  }

  private void writeSpecificationFileDefinition() {
    Path specificationsDirectory = Path.of(SPECIFICATION_FILES_PATH, "configuration");
    String[] extensionsToSearchFor = new String[] {"yaml"};
    Collection<File> files = FileUtils.listFiles(new File(specificationsDirectory.toUri()), extensionsToSearchFor, false);

    List<String> listOfFileNames = files.stream().map(File::getName).sorted().collect(Collectors.toList());

    Path specificationDefinitionPath = Path.of(SECRETS_MODULE_PATH_PREFIX, "SecretsSpecificationFilesDefinition.java");
    Path specificationDefinitionTemplatePath = Path.of(TEMPLATE_PATH_PREFIX, "SecretsSpecificationFilesDefinitionTemplate.java");

    try {
      Files.copy(specificationDefinitionTemplatePath, specificationDefinitionPath, StandardCopyOption.REPLACE_EXISTING);

      String content = Files.readString(specificationDefinitionPath, charset);
      content = content.replace("//<REPLACE-WITH-SET-OF-FILENAMES>", generateSpecificationDefinitionSet(listOfFileNames));
      Files.write(specificationDefinitionPath, content.getBytes(charset));
    } catch (IOException e) {
      LOG.error("Error while writing SecretsSpecificationFilesDefinition", e);
      throw new RuntimeException(e);
    }

    LOG.info("Successfully generated SecretsSpecificationFilesDefinition");
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
    Path checkPath = Path.of(CHECK_PATH_PREFIX, checkName + ".java");
    Path checkTestPath = Path.of(CHECK_TESTS_PATH_PREFIX, checkName + "Test.java");
    Path rspecJson = Path.of(RSPEC_FILES_PATH_PREFIX, rspecKey + ".json");
    Path rspecHtml = Path.of(RSPEC_FILES_PATH_PREFIX, rspecKey + ".html");

    try {
      Files.deleteIfExists(checkPath);
      Files.deleteIfExists(checkTestPath);
      Files.deleteIfExists(rspecJson);
      Files.deleteIfExists(rspecHtml);
    } catch (IOException e) {
      LOG.error("Error while deleting Check with name \"" + checkName + "\", please fix manually", e);
      throw new RuntimeException(e);
    }

    LOG.info("Successfully removed Check \"{}\" with rspecKey %s", checkName);
  }

  private void writeUpdatedRulesDefinition(List<String> checkNames, Set<String> checkNamesNotUsedAnymore) {
    Set<String> uniqueCheckNames = new SecretsCheckList().checks().stream().map(Class::getSimpleName).collect(Collectors.toSet());
    uniqueCheckNames.addAll(checkNames);
    uniqueCheckNames.removeAll(checkNamesNotUsedAnymore);

    List<String> updatedCheckNames = new ArrayList<>(uniqueCheckNames);

    Collections.sort(updatedCheckNames);

    Path checkTestTemplatePath = Path.of(TEMPLATE_PATH_PREFIX, "SecretsCheckListTemplate.java");
    Path rulesDefPath = Path.of(SECRETS_MODULE_PATH_PREFIX, "SecretsCheckList.java");
    try {
      Files.copy(checkTestTemplatePath, rulesDefPath, StandardCopyOption.REPLACE_EXISTING);

      String content = Files.readString(rulesDefPath, charset);
      content = content.replace("//<REPLACE-WITH-IMPORTS-OF-ALL-CHECKS>", generateImportsFor(updatedCheckNames));
      content = content.replace("//<REPLACE-WITH-LIST-OF-CHECKS>", generateChecksMethodFor(updatedCheckNames));
      Files.write(rulesDefPath, content.getBytes(charset));
    } catch (IOException e) {
      LOG.error("Error when updating SecretRulesDefinition.java, please fix manually", e);
      throw new RuntimeException(e);
    }

    LOG.info("Successfully updated SecretRulesDefinition.java");
  }

  private String generateChecksMethodFor(List<String> checkNames) {
    StringBuilder sb = new StringBuilder();

    sb.append("private static final List<Class<?>> SECRET_CHECKS = List.of(");
    sb.append(System.lineSeparator());
    for (int i = 0; i < checkNames.size(); i++) {
      sb.append("  ");
      sb.append(checkNames.get(i));
      sb.append(".class");
      if (i == checkNames.size() - 1) {
        sb.append(");");
      } else {
        sb.append(",");
        sb.append(System.lineSeparator());
      }
    }
    return sb.toString();
  }

  private String generateSpecificationDefinitionSet(List<String> fileNames) {
    StringBuilder sb = new StringBuilder();

    sb.append("public static Set<String> existingSecretSpecifications() {");
    sb.append(System.lineSeparator());
    sb.append("    return Set.of(");
    sb.append(System.lineSeparator());
    for (int i = 0; i < fileNames.size(); i++) {
      sb.append("      \"" + fileNames.get(i) + "\"");
      if (i == fileNames.size() - 1) {
        sb.append(");");
      } else {
        sb.append(",");
      }
      sb.append(System.lineSeparator());
    }
    sb.append("  }");
    return sb.toString();
  }

  private String generateImportsFor(List<String> checkNames) {
    StringBuilder sb = new StringBuilder();

    for (String checkName : checkNames) {
      sb.append("import org.sonar.plugins.secrets.checks.");
      sb.append(checkName);
      sb.append(";");
      sb.append(System.lineSeparator());
    }
    return sb.toString();
  }

  private void constructFileForRulesAPI(Set<String> keysToUpdateRuleAPIFor) {
    Path pathToWriteUpdateFileTo = Path.of(TEMPLATE_PATH_PREFIX, "rspecKeysToUpdate.txt");
    String content = generateContentForRulesAPIUpdateFile(keysToUpdateRuleAPIFor);

    try {
      Files.write(pathToWriteUpdateFileTo, content.getBytes(charset));
    } catch (IOException e) {
      LOG.error("Error when trying to write update file ruleAPI, please fix manually", e);
      throw new RuntimeException(e);
    }

    LOG.info("Successfully generated rule-api update file");
  }

  private String generateContentForRulesAPIUpdateFile(Set<String> keysToUpdateRuleAPIFor) {
    StringBuilder sb = new StringBuilder();

    for (String key : keysToUpdateRuleAPIFor) {
      sb.append(key);
      sb.append(System.lineSeparator());
    }
    return sb.toString();
  }

  private void testDeserializationOfSpecificationFiles() {
    new SpecificationLoader();
    List<LogAndArguments> errorLogs = logTester.getLogs(Level.ERROR);
    if (!errorLogs.isEmpty()) {
      throw new RuntimeException(failMessage(errorLogs));
    }
  }

  private String failMessage(List<LogAndArguments> errorLogs) {
    StringBuilder sb = new StringBuilder();
    sb.append("Generation process failed because of: ");
    sb.append(System.lineSeparator());
    for (LogAndArguments errorLog : errorLogs) {
      sb.append("- ");
      sb.append(errorLog.getFormattedMsg());
      sb.append(System.lineSeparator());
    }
    return sb.toString();
  }
}
