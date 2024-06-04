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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.sonar.api.testfixtures.log.LogAndArguments;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonar.plugins.common.Check;
import org.sonar.plugins.secrets.SecretsCheckList;
import org.sonar.plugins.secrets.api.SpecificationLoader;
import org.sonar.plugins.secrets.configuration.model.Rule;

// This name is intentionally not ending in "Test" to not get picked up automatically by maven
@SuppressWarnings("java:S3577")
class UpdatingSpecificationFilesGenerator {

  private final static String CHECK_TESTS_PATH_PREFIX = String.join(File.separator, "src", "test", "java", "org", "sonar", "plugins", "secrets", "checks");
  private final static String SECRETS_MODULE_PATH_PREFIX = String.join(File.separator, "src", "main", "java", "org", "sonar", "plugins", "secrets");
  private final static String SECRETS_MODULE_RESOURCE_PATH_PREFIX = String.join(File.separator, "src", "main", "resources", "org", "sonar");
  private final static String CHECK_PATH_PREFIX = String.join(File.separator, SECRETS_MODULE_PATH_PREFIX, "checks");
  private final static String TEMPLATE_PATH_PREFIX = String.join(File.separator, "src", "test", "resources", "templates");
  private final static String RSPEC_FILES_PATH_PREFIX = String.join(File.separator, SECRETS_MODULE_RESOURCE_PATH_PREFIX, "l10n", "secrets", "rules", "secrets");
  private final Charset charset = StandardCharsets.UTF_8;
  private static final Logger LOG = LoggerFactory.getLogger(UpdatingSpecificationFilesGenerator.class);

  @RegisterExtension
  LogTesterJUnit5 logTester = new LogTesterJUnit5();

  @Test
  @EnabledIfEnvironmentVariable(named = "GENERATION_ENABLED", matches = "true", disabledReason = "This test should not be executed during a normal test run")
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

    removeUnusedChecks(keysNotUsedAnymore, existingKeysMappedToFileName);
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
      LOG.error("Error while writing Check file with name \"{}.java\", please fix manually", checkName, e);
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
      LOG.error("Error while writing Check Test file with name \"{}Test.java\", please fix manually", checkName, e);
      throw new RuntimeException(e);
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
      LOG.error("Error while deleting Check with name \"{}\", please fix manually", checkName, e);
      throw new RuntimeException(e);
    }

    LOG.info("Successfully removed Check \"{}\" with rspecKey %s", checkName);
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
