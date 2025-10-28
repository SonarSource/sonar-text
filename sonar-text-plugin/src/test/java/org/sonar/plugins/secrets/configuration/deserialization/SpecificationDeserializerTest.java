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
package org.sonar.plugins.secrets.configuration.deserialization;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import java.io.InputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.sonar.plugins.secrets.configuration.model.Specification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.sonar.plugins.secrets.utils.SmileConverter.convertYamlToSmileStream;

class SpecificationDeserializerTest {

  @Test
  void deserializeMinSpecifications() {
    String fileName = "validMinSpec.sml";
    InputStream specificationStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("secretsConfiguration/" + fileName);

    Specification result = SpecificationDeserializer.deserialize(specificationStream, fileName);
    Specification expected = ReferenceTestModel.constructMinimumSpecification();

    assertThat(result).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void deserializeReferenceSpecifications() {
    String fileName = "validReferenceSpec.sml";
    InputStream specificationStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("secretsConfiguration/" + fileName);

    Specification result = SpecificationDeserializer.deserialize(specificationStream, fileName);
    Specification expected = ReferenceTestModel.constructReferenceSpecification();

    assertThat(result).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void throwExceptionOnInvalidFile() {
    String fileName = "invalidSpecWithUnexpectedFieldFailsDuringDeserialization.yaml";
    InputStream specificationStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("secretsConfiguration/" + fileName);

    assertThatExceptionOfType(DeserializationException.class)
      .isThrownBy(() -> SpecificationDeserializer.deserialize(specificationStream, fileName))
      .withMessage(String.format("Deserialization of specification failed for file: %s", fileName));
  }

  @Test
  void throwExceptionOnMissingFile() {
    String specificationFileName = "doesNotExist.yaml";
    InputStream specificationStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("secretsConfiguration/" + specificationFileName);

    assertThatExceptionOfType(DeserializationException.class)
      .isThrownBy(() -> SpecificationDeserializer.deserialize(specificationStream, specificationFileName))
      .withMessage(String.format(
        "Deserialization of specification failed for file because it was not found: %s", specificationFileName));
  }

  @ParameterizedTest
  @ValueSource(strings = {
    """
      provider:
        detection:
          post:
            name: base
            patternNot:
              - test
      """,
    """
      provider:
        detection:
          post:
            name: base
            patternNot:
              - test
            groups:
              - name: sub
                patternNot:
                  - test
      """,
  })
  void shouldThrowExceptionOnNamedTopLevelPostModule(String spec) {
    var specificationStream = convertYamlToSmileStream(spec);

    assertThatExceptionOfType(DeserializationException.class)
      .isThrownBy(() -> SpecificationDeserializer.deserialize(specificationStream, "test.yaml"))
      .withMessage("Deserialization of specification failed for file: %s".formatted("test.yaml"))
      .havingCause()
      .isInstanceOf(UnrecognizedPropertyException.class)
      .withMessageContaining("Unrecognized field \"name\"");
  }

  @ParameterizedTest
  @ValueSource(strings = {
    """
      provider:
        detection:
          post:
            groups:
              - patternNot:
                  - test
      """,
    """
      provider:
        detection:
          post:
            groups:
              - name: sub
                patternNot:
                  - test
                groups:
                  - name: subsub
                    patternNot:
                      - test
      """
  })
  void shouldThrowExceptionMalformedGroupElement(String spec) {
    var specificationStream = convertYamlToSmileStream(spec);

    assertThatExceptionOfType(DeserializationException.class)
      .isThrownBy(() -> SpecificationDeserializer.deserialize(specificationStream, "test.yaml"))
      .withMessage("Deserialization of specification failed for file: %s".formatted("test.yaml"))
      .havingCause()
      .isInstanceOf(JsonMappingException.class);
  }
}
