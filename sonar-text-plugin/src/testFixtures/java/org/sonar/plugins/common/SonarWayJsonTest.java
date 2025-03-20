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
package org.sonar.plugins.common;

import com.networknt.schema.InputFormat;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.PathType;
import com.networknt.schema.SchemaValidatorsConfig;
import com.networknt.schema.SpecVersion;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.fail;

public class SonarWayJsonTest {

  public static final JsonSchema JSON_SCHEMA;

  static {
    var config = SchemaValidatorsConfig.builder()
      .pathType(PathType.JSON_PATH)
      .locale(Locale.ENGLISH)
      .build();
    JSON_SCHEMA = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012).getSchema("", config);
  }

  public static void validateJson(Path path) throws IOException {
    var content = Files.readString(path);
    try {
      JSON_SCHEMA.validate(content, InputFormat.JSON);
    } catch (Exception e) {
      fail("Failed to validate SonarWay profile at \"%s\"".formatted(path), e);
    }
  }

  @Test
  void shouldValidateSecretsSonarWayJson() throws IOException {
    var path = Path.of("src", "main", "resources", "org", "sonar", "l10n", "secrets", "rules", "secrets", "Sonar_way_profile.json");
    validateJson(path);
  }

  @Test
  void shouldValidateTextSonarWayJson() throws IOException {
    var path = Path.of("src", "main", "resources", "org", "sonar", "l10n", "text", "rules", "text", "Sonar_way_profile.json");
    validateJson(path);
  }
}
