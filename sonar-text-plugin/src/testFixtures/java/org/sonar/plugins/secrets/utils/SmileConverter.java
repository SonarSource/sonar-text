/*
 * SonarQube Text Plugin
 * Copyright (C) 2021-2026 SonarSource Sàrl
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
package org.sonar.plugins.secrets.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class SmileConverter {

  private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());
  private static final ObjectMapper SMILE_MAPPER = new ObjectMapper(new SmileFactory());

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
