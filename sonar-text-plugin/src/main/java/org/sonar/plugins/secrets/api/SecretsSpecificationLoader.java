/*
 * SonarQube Text Plugin
 * Copyright (C) 2021-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
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

import java.io.InputStream;
import java.util.ArrayList;
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

/**
 * A class that loads secret detection specifications from YAML files.
 */
public class SecretsSpecificationLoader implements SpecificationLoader {
  private static final Logger LOG = LoggerFactory.getLogger(SecretsSpecificationLoader.class);

  public static final String DEFAULT_SPECIFICATION_LOCATION = "org/sonar/plugins/secrets/configuration/";
  public static final ExceptionHandler DEFAULT_EXCEPTION_HANDLER = (e, specificationFileName) -> LOG.warn("{}: Could not load specification from file: {}",
    e.getClass().getSimpleName(), specificationFileName);
  private final Map<String, List<Rule>> rulesMappedToKey = new HashMap<>();
  private final Map<String, List<String>> keyMappedToFiles = new HashMap<>();
  private final ExceptionHandler exceptionHandler;

  /**
   * Creates a new SpecificationLoader pointing to the default location and default set of specification files.
   */
  public SecretsSpecificationLoader() {
    this(DEFAULT_SPECIFICATION_LOCATION, existingSecretSpecifications(), DEFAULT_EXCEPTION_HANDLER);
  }

  /**
   * Creates a new SpecificationLoader pointing to a specific location.
   * @param specificationLocation Directory containing the specification files.
   * @param specifications Set of specification file names.
   */
  public SecretsSpecificationLoader(String specificationLocation, Set<String> specifications) {
    this(specificationLocation, specifications, DEFAULT_EXCEPTION_HANDLER);
  }

  /**
   * Create a new SpecificationLoader.
   * @param specificationLocation Directory containing the specification files.
   * @param specifications Set of specification file names.
   * @param exceptionHandler Handler for exceptions that occur during specification loading.
   */
  public SecretsSpecificationLoader(String specificationLocation, Set<String> specifications, ExceptionHandler exceptionHandler) {
    this.exceptionHandler = exceptionHandler;

    loadSpecifications(specificationLocation, specifications);
  }

  /**
   * Create a new SpecificationLoader.
   * @param specificationsByLocation A map of specification locations (directories containing the specification files) to sets of specification file names.
   * @param exceptionHandler Handler for exceptions that occur during specification loading.
   */
  public SecretsSpecificationLoader(Map<String, Set<String>> specificationsByLocation, ExceptionHandler exceptionHandler) {
    this.exceptionHandler = exceptionHandler;

    for (Map.Entry<String, Set<String>> entry : specificationsByLocation.entrySet()) {
      loadSpecifications(entry.getKey(), entry.getValue());
    }
  }

  /**
   * Load a list of specifications.
   * For each loaded {@link Specification} object, the specification object and the name of the file it was loaded from
   * will be stored in the {@link #rulesMappedToKey} and {@link #keyMappedToFiles} maps.
   */
  private void loadSpecifications(String specificationLocation, Set<String> specifications) {
    for (String specificationFileName : specifications) {
      Specification specification;
      try {
        specification = loadSpecification(specificationLocation, specificationFileName);
      } catch (DeserializationException | SchemaValidationException e) {
        exceptionHandler.handle(e, specificationFileName);
        continue;
      }
      for (Rule rule : specification.getProvider().getRules()) {
        this.rulesMappedToKey.computeIfAbsent(rule.getRspecKey(), k -> new ArrayList<>()).add(rule);
        this.keyMappedToFiles.computeIfAbsent(rule.getRspecKey(), k -> new ArrayList<>()).add(specificationFileName);
      }
    }
  }

  private static Specification loadSpecification(String specificationLocation, String fileName) {
    InputStream specificationStream = SecretsSpecificationLoader.class.getClassLoader()
      .getResourceAsStream(specificationLocation + fileName);
    return SpecificationDeserializer.deserialize(specificationStream, fileName);
  }

  /**
   * Provide the list of rules associated to a given rule ID.
   * @param key Rule ID
   * @return The list of rules that were loaded from the specifications related to the key.
   */
  public List<Rule> getRulesForKey(String key) {
    return rulesMappedToKey.getOrDefault(key, new ArrayList<>());
  }

  /**
   * Provide the list of specification filenames associated to a given key.
   * @param key Key to look for, with format SXXXX.
   * @return The list of filename that were used to load specifications related to the key.
   */
  public List<String> getSpecificationFilesForKey(String key) {
    return keyMappedToFiles.getOrDefault(key, new ArrayList<>());
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
