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
package org.sonar.plugins.secrets.configuration.deserialization;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.sonar.plugins.secrets.configuration.model.Specification;

public class SpecificationDeserializer {

  private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory());

  private static final Set<String> SPECIFICATIONS = Set.of("minSpec.yaml");

  private SpecificationDeserializer() {
  }

  public static List<Specification> deserializeProvidedSpecifications() {
    ArrayList<Specification> specifications = new ArrayList<>();
    for (String specification : SPECIFICATIONS) {
      URL specificationUrl = SpecificationDeserializer.class.getResource(specification);
      specifications.add(deserialize(specificationUrl));
    }
    return specifications;
  }

  static Specification deserialize(URL specification) {
    try {
      return MAPPER.readValue(specification, Specification.class);
    } catch (IOException e) {
      String filePath = specification.getPath();
      String fileName = filePath.substring(filePath.lastIndexOf('/') + 1);
      throw new SpecificationDeserializationException(String.format("Deserialization of specification failed for file: %s", fileName), e);
    }
  }
}
