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
package org.sonar.plugins.secrets.api;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.plugins.secrets.configuration.deserialization.DeserializationException;
import org.sonar.plugins.secrets.configuration.deserialization.SpecificationDeserializer;
import org.sonar.plugins.secrets.configuration.model.Rule;
import org.sonar.plugins.secrets.configuration.model.Specification;
import org.sonar.plugins.secrets.configuration.validation.SchemaValidationException;

public class SpecificationLoader {
  private static final Logger LOG = Loggers.get(SpecificationLoader.class);

  public static final String DEFAULT_SPECIFICATION_LOCATION = "org/sonar/plugins/secrets/configuration/";
  private final Map<String, List<Rule>> rulesMappedToKey;

  public SpecificationLoader() {
    this(DEFAULT_SPECIFICATION_LOCATION, defaultSpecifications());
  }

  public SpecificationLoader(String specificationLocation, Set<String> specifications) {
    rulesMappedToKey = initialize(specificationLocation, specifications);
  }

  private static Map<String, List<Rule>> initialize(String specificationLocation, Set<String> specifications) {
    Map<String, List<Rule>> keyToRule = new HashMap<>();

    for (String specificationFileName : specifications) {
      Specification specification;
      try {
        specification = loadSpecification(specificationLocation, specificationFileName);
      } catch (DeserializationException | SchemaValidationException e) {
        LOG.error("{}: Could not load specification from file: {}", e.getClass().getSimpleName(), specificationFileName);
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

  Map<String, List<Rule>> getRulesMappedToKey() {
    return rulesMappedToKey;
  }

  public static Set<String> defaultSpecifications() {
    return Set.of(
      "alibaba.yaml",
      "aws.yaml",
      "azure.yaml",
      "clarifai.yaml",
      "django.yaml",
      "facebook.yaml",
      "gcp.yaml",
      "github.yaml",
      "gitlab.yaml",
      "google-api.yaml",
      "google-oauth2.yaml",
      "google-recaptcha.yaml",
      "ibm.yaml",
      "mongodb.yaml",
      "mws.yaml",
      "mysql.yaml",
      "odbc.yaml",
      "openweathermap.yaml",
      "postgresql.yaml",
      "pubkey-crypto.yaml",
      "rapidapi.yaml",
      "riot.yaml",
      "sendgrid.yaml",
      "sonarqube.yaml",
      "spotify.yaml",
      "ssh.yaml",
      "telegram.yaml",
      "wechat.yaml");
  }
}
