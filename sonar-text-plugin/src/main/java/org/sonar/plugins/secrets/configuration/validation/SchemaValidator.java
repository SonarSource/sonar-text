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
package org.sonar.plugins.secrets.configuration.validation;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Set;

public class SchemaValidator {

  private static final String FILE_LOCATION = "/org/sonar/plugins/secrets/configuration/";
  private static final String VALIDATION_SCHEMA_FILE = "specification-json-schema.json";
  private static final Set<String> SPECIFICATION_FILES = Set.of("minSpec.yaml");

  private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory());

  private static final JsonSchema VALIDATION_SCHEMA = initializeValidationSchema();

  private SchemaValidator() {

  }

  private static JsonSchema initializeValidationSchema() {
    JsonSchemaFactory schemaFactory = JsonSchemaFactory
      .builder(JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012))
      .objectMapper(MAPPER)
      .build();
    InputStream validationSchema = SchemaValidator.class.getResourceAsStream(FILE_LOCATION + VALIDATION_SCHEMA_FILE);
    return schemaFactory.getSchema(validationSchema);
  }

  public static void validateConfigurationFiles() {
    for (String configurationFile : SPECIFICATION_FILES) {
      URL configuration = SchemaValidator.class.getResource(FILE_LOCATION + configurationFile);
      validate(configuration);
    }
  }

  public static void validate(URL configurationLocation) {
    try {
      Set<ValidationMessage> validate = VALIDATION_SCHEMA.validate(MAPPER.readTree(configurationLocation));
      if (!validate.isEmpty()) {
        String filePath = configurationLocation.getPath();
        String fileName = filePath.substring(filePath.lastIndexOf('/') + 1);
        String errorMessage = String.format("Specification file \"%s\" failed the schema validation", fileName);
        throw new SchemaValidationException(errorMessage);
      }
    } catch (IOException e) {
      throw new SchemaValidationException(e);
    }
  }
}
