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

import java.net.URL;
import org.junit.jupiter.api.Test;
import org.sonar.plugins.secrets.configuration.model.Specification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class SpecificationDeserializerTest {

  @Test
  void deserializeMinSpecifications() {
    URL specificationUrl = Thread.currentThread().getContextClassLoader().getResource("secretsConfiguration/validMinSpec.yaml");

    Specification result = SpecificationDeserializer.deserialize(specificationUrl);
    Specification expected = ReferenceTestModel.constructMinimumSpecification();

    assertThat(result).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void deserializeReferenceSpecifications() {
    URL specificationUrl = Thread.currentThread().getContextClassLoader().getResource("secretsConfiguration/validReferenceSpec.yaml");

    Specification result = SpecificationDeserializer.deserialize(specificationUrl);
    Specification expected = ReferenceTestModel.constructReferenceSpecification();

    assertThat(result).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void throwExceptionOnInvalidFile() {
    URL specificationUrl = Thread.currentThread().getContextClassLoader().getResource("secretsConfiguration/invalidEmptySpec.yaml");

    assertThatExceptionOfType(DeserializationException.class)
      .isThrownBy(() -> SpecificationDeserializer.deserialize(specificationUrl))
      .withMessage(String.format("Deserialization of specification failed for file: %s", "invalidEmptySpec.yaml"));
  }
}
