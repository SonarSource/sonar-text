/*
 * SonarQube Text Plugin
 * Copyright (C) 2021-2025 SonarSource SA
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
package org.sonar.plugins.secrets.configuration.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.io.InputStream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

class SchemaValidatorTest {

  private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory());

  @ParameterizedTest
  @ValueSource(strings = {"validMinSpec.yaml", "validReferenceSpec.yaml"})
  void testSpecificationFilesAreValid(String specificationFileName) throws IOException {
    InputStream specificationStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("secretsConfiguration/" + specificationFileName);
    JsonNode specification = MAPPER.readTree(specificationStream);

    assertThatNoException().isThrownBy(() -> SchemaValidator.validateSpecification(specification, specificationFileName));
  }

  @ParameterizedTest
  @ValueSource(strings = {"invalidEmptySpec.yaml", "invalidSpecMissingRequiredField.yaml",
    "invalidSpecWithUnexpectedFieldFailsDuringValidation.yaml",
    "invalidSpecWithWrongType.yaml",
    "invalidSpecForbiddenCategory.yaml"})
  void testSpecificationFilesAreInValid(String specificationFileName) throws IOException {
    InputStream specificationStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("secretsConfiguration/" + specificationFileName);
    JsonNode specification = MAPPER.readTree(specificationStream);

    assertThatExceptionOfType(SchemaValidationException.class)
      .isThrownBy(() -> SchemaValidator.validateSpecification(specification, specificationFileName))
      .withMessage(String.format("Specification file \"%s\" failed the schema validation", specificationFileName));
  }
}
