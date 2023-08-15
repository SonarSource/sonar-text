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
package org.sonar.plugins.secrets.configuration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.io.InputStream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.sonar.plugins.secrets.configuration.validation.SchemaValidator;

import static org.assertj.core.api.Assertions.assertThatNoException;

class SpecificationValidationTest {

  private static final String SPECIFICATIONS_METHOD_NAME = "org.sonar.plugins.secrets.SecretsSpecificationFilesDefinition#existingSecretSpecifications";
  private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory());

  @ParameterizedTest
  @MethodSource(SPECIFICATIONS_METHOD_NAME)
  void validateSpecificationFiles(String fileName) throws IOException {
    InputStream specificationStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("org/sonar/plugins/secrets/configuration/" + fileName);
    JsonNode specification = MAPPER.readTree(specificationStream);

    assertThatNoException().isThrownBy(() -> SchemaValidator.validate(specification, fileName));
  }
}
