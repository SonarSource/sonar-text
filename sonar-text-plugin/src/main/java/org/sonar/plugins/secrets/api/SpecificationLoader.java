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
import java.util.Collections;
import java.util.HashMap;
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
  private final Map<String, Rule> rulesMappedToKey;

  public SpecificationLoader() {
    this("org/sonar/plugins/secrets/configuration/", Collections.emptySet());
  }

  public SpecificationLoader(String specificationLocation, Set<String> specifications) {
    rulesMappedToKey = initialize(specificationLocation, specifications);
  }

  private Map<String, Rule> initialize(String specificationLocation, Set<String> specifications) {
    Map<String, Rule> keyToRule = new HashMap<>();

    for (String specificationFileName : specifications) {
      Specification specification;
      try {
        specification = loadSpecification(specificationLocation, specificationFileName);
      } catch (DeserializationException | SchemaValidationException e) {
        LOG.error(String.format("Could not load specification from file: %s", specificationFileName), e);
        continue;
      }

      for (Rule rule : specification.getProvider().getRules()) {
        if (keyToRule.put(rule.getId(), rule) != null) {
          String errorMessage = String.format(
            "RuleKey %s was used multiple times, when it should be unique across all specification files.", rule.getId());
          throw new SchemaValidationException(errorMessage);
        }
      }
    }
    return keyToRule;
  }

  private static Specification loadSpecification(String specificationLocation, String fileName) {
    InputStream specificationStream = SpecificationLoader.class.getClassLoader()
      .getResourceAsStream(specificationLocation + fileName);
    return SpecificationDeserializer.deserialize(specificationStream, fileName);
  }

  public Rule getRuleForKey(String key) {
    return rulesMappedToKey.get(key);
  }

}
