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
package org.sonar.plugins.secrets.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.smile.SmileMapper;
import tools.jackson.dataformat.yaml.YAMLMapper;

public class SmileConverter {

  private static final ObjectMapper YAML_MAPPER = YAMLMapper.builder()
    .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    .build();
  private static final ObjectMapper SMILE_MAPPER = SmileMapper.builder()
    .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    .build();

  private SmileConverter() {
  }

  public static InputStream convertYamlToSmileStream(String yamlContent) {
    try {
      var jsonNode = YAML_MAPPER.readTree(yamlContent);
      var outputStream = new ByteArrayOutputStream();
      SMILE_MAPPER.writeValue(outputStream, jsonNode);
      return new ByteArrayInputStream(outputStream.toByteArray());
    } catch (Exception e) {
      throw new RuntimeException("Failed to convert YAML to Smile", e);
    }
  }
}
