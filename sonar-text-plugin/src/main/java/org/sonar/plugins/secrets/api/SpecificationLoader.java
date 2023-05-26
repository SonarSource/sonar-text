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
import org.sonar.plugins.secrets.configuration.deserialization.SpecificationDeserializer;
import org.sonar.plugins.secrets.configuration.model.Rule;
import org.sonar.plugins.secrets.configuration.model.Specification;
import org.sonar.plugins.secrets.configuration.validation.SchemaValidationException;

public class SpecificationLoader {

  private static String specificationLocation = "org/sonar/plugins/secrets/configuration/";
  private static Set<String> specifications = Collections.emptySet();
  private static Map<String, Rule> rulesMappedToKey = initialize();

  private SpecificationLoader() {
  }

  protected static void reinitialize(String specificationLocation, Set<String> specifications) {
    SpecificationLoader.specificationLocation = specificationLocation;
    SpecificationLoader.specifications = specifications;
    SpecificationLoader.rulesMappedToKey = initialize();
  }

  private static Map<String, Rule> initialize() {
    Map<String, Rule> keyToRule = new HashMap<>();

    for (String specificationFileName : specifications) {
      Specification specification = loadSpecification(specificationFileName);
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

  private static Specification loadSpecification(String fileName) {
    InputStream specificationStream = SpecificationLoader.class.getClassLoader()
      .getResourceAsStream(specificationLocation + fileName);
    return SpecificationDeserializer.deserialize(specificationStream, fileName);
  }

  public static Rule getRuleForKey(String key) {
    return rulesMappedToKey.get(key);
  }

}
