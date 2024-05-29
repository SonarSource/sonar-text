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
package org.sonar.plugins.secrets.api;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.plugins.secrets.configuration.deserialization.DeserializationException;
import org.sonar.plugins.secrets.configuration.deserialization.SpecificationDeserializer;
import org.sonar.plugins.secrets.configuration.model.Rule;
import org.sonar.plugins.secrets.configuration.model.Specification;
import org.sonar.plugins.secrets.configuration.validation.SchemaValidationException;

import static org.sonar.plugins.secrets.SecretsSpecificationFilesDefinition.existingSecretSpecifications;

public class SpecificationLoader {
  private static final Logger LOG = LoggerFactory.getLogger(SpecificationLoader.class);

  public static final String DEFAULT_SPECIFICATION_LOCATION = "org/sonar/plugins/secrets/configuration/";
  public static final ExceptionHandler DEFAULT_EXCEPTION_HANDLER = (e, specificationFileName) -> LOG.warn("{}: Could not load specification from file: {}",
    e.getClass().getSimpleName(), specificationFileName);
  private final Map<String, List<Rule>> rulesMappedToKey;
  private final ExceptionHandler exceptionHandler;

  public SpecificationLoader() {
    this(DEFAULT_SPECIFICATION_LOCATION, existingSecretSpecifications(), DEFAULT_EXCEPTION_HANDLER);
  }

  public SpecificationLoader(String specificationLocation, Set<String> specifications) {
    this(specificationLocation, specifications, DEFAULT_EXCEPTION_HANDLER);
  }

  /**
   * Create a new SpecificationLoader.
   * @param specificationLocation Directory containing the specification files.
   * @param specifications Set of specification file names.
   * @param exceptionHandler Handler for exceptions that occur during specification loading.
   */
  public SpecificationLoader(String specificationLocation, Set<String> specifications, ExceptionHandler exceptionHandler) {
    this.exceptionHandler = exceptionHandler;
    rulesMappedToKey = initialize(specificationLocation, specifications);
  }

  private Map<String, List<Rule>> initialize(String specificationLocation, Set<String> specifications) {
    if (specifications.isEmpty()) {
      return Collections.emptyMap();
    }

    Map<String, List<Rule>> keyToRule = new HashMap<>();

    for (String specificationFileName : specifications) {
      Specification specification;
      try {
        specification = loadSpecification(specificationLocation, specificationFileName);
      } catch (DeserializationException | SchemaValidationException e) {
        exceptionHandler.handle(e, specificationFileName);
        continue;
      }

      for (Rule rule : specification.getProvider().getRules()) {
        keyToRule.computeIfAbsent(rule.getRspecKey(), k -> new ArrayList<>()).add(rule);
      }
    }
    return keyToRule;
  }

  private static Specification loadSpecification(String specificationLocation, String fileName) {
    InputStream specificationStream = SpecificationLoader.class.getClassLoader()
      .getResourceAsStream(specificationLocation + fileName);
    return SpecificationDeserializer.deserialize(specificationStream, fileName);
  }

  public List<Rule> getRulesForKey(String key) {
    return rulesMappedToKey.getOrDefault(key, new ArrayList<>());
  }

  public Map<String, List<Rule>> getRulesMappedToKey() {
    return rulesMappedToKey;
  }

  /**
   * A functional interface representing a handler for exceptions that occur during specification loading.
   */
  public interface ExceptionHandler {
    /**
     * Handle an exception that occurred during specification loading.
     * @param throwable The exception that occurred.
     * @param specificationFileName The name of the specification file that was being loaded.
     */
    void handle(Throwable throwable, String specificationFileName);
  }
}
