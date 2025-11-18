/*
 * SonarQube Text Plugin
 * Copyright (C) 2021-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.plugins.secrets.configuration.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.Error;
import com.networknt.schema.InputFormat;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import java.util.List;

public final class SchemaValidator {

  private static final String FILE_LOCATION = "/org/sonar/plugins/secrets/configuration/specifications/";
  private static final String VALIDATION_SCHEMA_FILE = "specification-json-schema.json";
  private static final Schema SPECIFICATION_VALIDATION_SCHEMA;

  private SchemaValidator() {
    // Utility class
  }

  static {
    var schemaRegistry = SchemaRegistry.withDefaultDialect(
      SpecificationVersion.DRAFT_2020_12,
      builder -> builder.resourceLoaders(loaders -> loaders.add(new ResourceSchemaLoader(FILE_LOCATION))));

    var validationSchema = SchemaValidator.class.getResourceAsStream(FILE_LOCATION + VALIDATION_SCHEMA_FILE);
    SPECIFICATION_VALIDATION_SCHEMA = schemaRegistry.getSchema(validationSchema, InputFormat.JSON);
  }

  public static void validateSpecification(JsonNode specification, String fileName) {
    var validate = validate(specification);
    if (!validate.isEmpty()) {
      var errorMessage = "Specification file \"%s\" failed the schema validation".formatted(fileName);
      throw new SchemaValidationException(errorMessage);
    }
  }

  public static List<Error> validate(JsonNode specification) {
    return SPECIFICATION_VALIDATION_SCHEMA.validate(specification);
  }
}
