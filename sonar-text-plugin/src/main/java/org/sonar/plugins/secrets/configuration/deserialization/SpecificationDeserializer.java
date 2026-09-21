/*
 * SonarQube Text Plugin
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.plugins.secrets.configuration.deserialization;

import java.io.InputStream;
import org.sonar.plugins.secrets.configuration.model.Specification;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.smile.SmileMapper;

public class SpecificationDeserializer {

  // Jackson 3 disables FAIL_ON_UNKNOWN_PROPERTIES by default; re-enable it to keep rejecting
  // specification files with typos/unexpected fields, as before the Jackson 2 -> 3 migration.
  private static final ObjectMapper MAPPER = SmileMapper.builder()
    .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    .build();

  private SpecificationDeserializer() {
  }

  public static Specification deserialize(InputStream specificationStream, String fileName) {
    try {
      JsonNode specification = MAPPER.readTree(specificationStream);
      return MAPPER.treeToValue(specification, Specification.class);
    } catch (IllegalArgumentException e) {
      throw new DeserializationException(
        String.format("Deserialization of specification failed for file because it was not found: %s", fileName), e);
    } catch (JacksonException e) {
      throw new DeserializationException(String.format("Deserialization of specification failed for file: %s", fileName), e);
    }
  }
}
