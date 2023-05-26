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

import java.io.InputStream;
import org.junit.jupiter.api.Test;
import org.sonar.plugins.secrets.configuration.model.Specification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class SpecificationDeserializerTest {

  @Test
  void deserializeMinSpecifications() {
    String fileName = "validMinSpec.yaml";
    InputStream specificationStream =
      Thread.currentThread().getContextClassLoader().getResourceAsStream("secretsConfiguration/" + fileName);

    Specification result = SpecificationDeserializer.deserialize(specificationStream, fileName);
    Specification expected = ReferenceTestModel.constructMinimumSpecification();

    assertThat(result).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void deserializeReferenceSpecifications() {
    String fileName = "validReferenceSpec.yaml";
    InputStream specificationStream =
      Thread.currentThread().getContextClassLoader().getResourceAsStream("secretsConfiguration/" + fileName);

    Specification result = SpecificationDeserializer.deserialize(specificationStream, fileName);
    Specification expected = ReferenceTestModel.constructReferenceSpecification();

    assertThat(result).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void throwExceptionOnInvalidFile() {
    String fileName = "invalidSpecWithUnexpectedFieldFailsDuringDeserialization.yaml";
    InputStream specificationStream =
      Thread.currentThread().getContextClassLoader().getResourceAsStream("secretsConfiguration/" + fileName);

    assertThatExceptionOfType(DeserializationException.class)
      .isThrownBy(() -> SpecificationDeserializer.deserialize(specificationStream, fileName))
      .withMessage(String.format("Deserialization of specification failed for file: %s", fileName));
  }

  @Test
  void throwExceptionOnMissingFile() {
    String specificationFileName = "doesNotExist.yaml";
    InputStream specificationStream =
      Thread.currentThread().getContextClassLoader().getResourceAsStream("secretsConfiguration/" + specificationFileName);

    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() -> SpecificationDeserializer.deserialize(specificationStream, specificationFileName));
  }
}
