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
package org.sonar.plugins.secrets.configuration.validation;

import java.io.InputStream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.yaml.YAMLMapper;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

class SchemaValidatorTest {

  private static final ObjectMapper MAPPER = YAMLMapper.builder()
    .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    .build();

  @ParameterizedTest
  @ValueSource(strings = {"validMinSpec.yaml", "validReferenceSpec.yaml"})
  void testSpecificationFilesAreValid(String specificationFileName) {
    InputStream specificationStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("secretsConfiguration/" + specificationFileName);
    JsonNode specification = MAPPER.readTree(specificationStream);

    assertThatNoException().isThrownBy(() -> SchemaValidator.validateSpecification(specification, specificationFileName));
  }

  @ParameterizedTest
  @ValueSource(strings = {"invalidEmptySpec.yaml", "invalidSpecMissingRequiredField.yaml",
    "invalidSpecWithUnexpectedFieldFailsDuringValidation.yaml",
    "invalidSpecWithWrongType.yaml",
    "invalidSpecForbiddenCategory.yaml",
    "invalidSpecEmptyNestedPost.yaml"})
  void testSpecificationFilesAreInValid(String specificationFileName) {
    InputStream specificationStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("secretsConfiguration/" + specificationFileName);
    JsonNode specification = MAPPER.readTree(specificationStream);

    assertThatExceptionOfType(SchemaValidationException.class)
      .isThrownBy(() -> SchemaValidator.validateSpecification(specification, specificationFileName))
      .withMessage(String.format("Specification file \"%s\" failed the schema validation", specificationFileName));
  }
}
