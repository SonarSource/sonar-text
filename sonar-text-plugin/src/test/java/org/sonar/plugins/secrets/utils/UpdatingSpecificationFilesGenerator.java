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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.sonar.api.internal.apachecommons.io.FileUtils;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.plugins.common.Check;
import org.sonar.plugins.secrets.SecretsRulesDefinition;
import org.sonar.plugins.secrets.api.SpecificationLoader;
import org.sonar.plugins.secrets.configuration.model.Rule;

// This name is intentionally not ending in "Test" to not get picked up automatically
@SuppressWarnings("java:S3577")
class UpdatingSpecificationFilesGenerator {

  private final Charset charset = StandardCharsets.UTF_8;
  private static final Logger LOG = Loggers.get(UpdatingSpecificationFilesGenerator.class);

  @Test
  @SuppressWarnings("java:S2699")
  void firstStep() {
    writeSpecificationFileDefinition();
  }

  @Test
  void secondStep() {

    SpecificationLoader specificationLoader = new SpecificationLoader();

    List<String> listOfAlreadyExistingKeys = retrieveAlreadyExistingKeys();
    Set<String> setOfExistingCheckClassNames = SecretsRulesDefinition.checks().stream().map(Class::getSimpleName).collect(Collectors.toSet());

    Map<String, List<Rule>> rulesMappedToKey = specificationLoader.getRulesMappedToKey();

    Set<String> keysToImplementChecksFor = rulesMappedToKey.keySet();
    keysToImplementChecksFor.removeAll(listOfAlreadyExistingKeys);

    List<String> newCheckNames = new ArrayList<>();
    for (String rspecKey : keysToImplementChecksFor) {
      String checkName = rulesMappedToKey.get(rspecKey).get(0).getProvider().getMetadata().getName();
      checkName = sanitizeCheckName(checkName, setOfExistingCheckClassNames);
      writeCheckFile(checkName, rspecKey);
      writeCheckTestFile(checkName);
      newCheckNames.add(checkName);
    }

    writeUpdatedRulesDefinition(newCheckNames);
  }

  List<String> retrieveAlreadyExistingKeys() {
    List<String> rspecKeys = new ArrayList<>();

    List<Class<?>> checks = SecretsRulesDefinition.checks();
    for (Class<?> check : checks) {
      Constructor<?>[] declaredConstructors = check.getDeclaredConstructors();
      Check instantiatedCheck;
      try {
        instantiatedCheck = (Check) declaredConstructors[0].newInstance();
      } catch (Exception e) {
        LOG.error("Error while retrieving already existing keys");
        throw new RuntimeException(e);
      }
      rspecKeys.add(instantiatedCheck.ruleKey.rule());
    }

    return rspecKeys;
  }

  private String sanitizeCheckName(String checkName, Set<String> existingClassNames) {
    checkName = checkName.replaceAll("[^a-zA-Z]", "");
    checkName = checkName + "Check";

    if (existingClassNames.contains(checkName)) {
      checkName = checkName.replace("Check", "UniqueNameCheck");
    }
    existingClassNames.add(checkName);
    return checkName;
  }

  private void writeCheckFile(String checkName, String rspecKey) {
    Path checkPath = Path.of("src", "main", "java", "org", "sonar", "plugins", "secrets", "checks", checkName + ".java");
    Path checkTemplatePath = Path.of("src", "test", "resources", "templates", "GenericCheckTemplate.java");
    try {
      Files.copy(checkTemplatePath, checkPath);

      String content = Files.readString(checkPath, charset);
      content = content.replaceAll("GenericCheckTemplate", checkName);
      content = content.replaceAll("<RSPEC-KEY>", rspecKey);
      Files.write(checkPath, content.getBytes(charset));
    } catch (IOException e) {
      LOG.error("Error while writing Check file with name \"" + checkName + ".java\", please fix manually", e);
      throw new RuntimeException(e);
    }

    String successMessage = String.format("Successfully generated Check \"%s.java\" with rspecKey %s", checkName, rspecKey);
    LOG.info(successMessage);
  }

  public void writeCheckTestFile(String checkName) {
    Path checkTestPath = Path.of("src", "test", "java", "org", "sonar", "plugins", "secrets", "checks", checkName + "Test.java");
    Path checkTestTemplatePath = Path.of("src", "test", "resources", "templates", "GenericCheckTestTemplate.java");
    try {
      Files.copy(checkTestTemplatePath, checkTestPath);

      String content = Files.readString(checkTestPath, charset);
      content = content.replaceAll("GenericCheckTemplate", checkName);
      content = content.replaceAll("GenericCheckTemplateTest", checkName + "Test");
      Files.write(checkTestPath, content.getBytes(charset));
    } catch (IOException e) {
      LOG.error("Error while writing Check Test file with name \"" + checkName + "Test.java\", please fix manually", e);
      throw new RuntimeException(e);
    }

    String successMessage = String.format("Successfully generated Check \"%sTest.java\"", checkName);
    LOG.info(successMessage);
  }

  public void writeSpecificationFileDefinition() {
    Path specificationsDirectoy = Path.of("src", "main", "resources", "org", "sonar", "plugins", "secrets", "configuration");
    String[] extensionsToSearchFor = new String[] {"yaml"};
    Collection<File> files = FileUtils.listFiles(new File(specificationsDirectoy.toUri()), extensionsToSearchFor, false);

    List<String> listOfFileNames = files.stream().map(File::getName).sorted().collect(Collectors.toList());

    Path specificationDefinitionPath = Path.of("src", "main", "java", "org", "sonar", "plugins", "secrets", "SecretsSpecificationFilesDefinition.java");
    Path specificationDefinitionTemplatePath = Path.of("src", "test", "resources", "templates", "SecretsSpecificationFilesDefinitionTemplate.java");

    try {
      Files.copy(specificationDefinitionTemplatePath, specificationDefinitionPath, StandardCopyOption.REPLACE_EXISTING);

      String content = Files.readString(specificationDefinitionPath, charset);
      content = content.replaceAll("<REPLACE-WITH-SET-OF-FILENAMES>", generateSpecificationDefinitionSet(listOfFileNames));
      Files.write(specificationDefinitionPath, content.getBytes(charset));
    } catch (IOException e) {
      LOG.error("Error while writing SecretsSpecificationFilesDefinition", e);
      throw new RuntimeException(e);
    }

    LOG.info("Successfully generated SecretsSpecificationFilesDefinition");
  }

  private void writeUpdatedRulesDefinition(List<String> checkNames) {
    Set<String> uniqueCheckNames = SecretsRulesDefinition.checks().stream().map(Class::getSimpleName).collect(Collectors.toSet());
    uniqueCheckNames.addAll(checkNames);

    List<String> updatedCheckNames = new ArrayList<>(uniqueCheckNames);

    Collections.sort(updatedCheckNames);

    Path checkTestTemplatePath = Path.of("src", "test", "resources", "templates", "SecretsRulesDefinitionTemplate.java");
    Path rulesDefPath = Path.of("src", "main", "java", "org", "sonar", "plugins", "secrets", "SecretsRulesDefinition.java");
    try {
      Files.copy(checkTestTemplatePath, rulesDefPath, StandardCopyOption.REPLACE_EXISTING);

      String content = Files.readString(rulesDefPath, charset);
      content = content.replace("<REPLACE-WITH-IMPORTS-OF-ALL-CHECKS>", generateImportsFor(updatedCheckNames));
      content = content.replaceAll("<REPLACE-WITH-LIST-OF-CHECKS>", generateChecksMethodFor(updatedCheckNames));
      Files.write(rulesDefPath, content.getBytes(charset));
    } catch (IOException e) {
      LOG.error("Error when updating SecretRulesDefinition.java, please fix manually", e);
      throw new RuntimeException(e);
    }

    LOG.info("Successfully updated SecretRulesDefinition.java");
  }

  private String generateChecksMethodFor(List<String> checkNames) {
    StringBuilder sb = new StringBuilder();

    sb.append("public static List<Class<?>> checks() {");
    sb.append(System.lineSeparator());
    sb.append("    return List.of(");
    sb.append(System.lineSeparator());
    for (int i = 0; i < checkNames.size(); i++) {
      sb.append("      " + checkNames.get(i) + ".class");
      if (i == checkNames.size() - 1) {
        sb.append(");");
      } else {
        sb.append(",");
      }
      sb.append(System.lineSeparator());
    }
    sb.append("  }");
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
}
