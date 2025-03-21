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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.fail;

public class SonarWayJsonHelper {

  public static void validateJson(Path path) throws IOException {
    var content = Files.readString(path);
    var duplicatedKeys = findDuplicatedKeys(content);
    if (!duplicatedKeys.isEmpty()) {
      var printableKeys = String.join(", ", duplicatedKeys);
      fail("Duplicated rule keys in %s keys: %s".formatted(path, printableKeys));
    }
  }

  private static HashSet<String> findDuplicatedKeys(String content) {
    var objectMapper = new ObjectMapper();
    JsonNode root = null;
    try {
      root = objectMapper.readTree(content);
    } catch (Exception e) {
      fail("Failed to parse JSON content", e);
    }
    var ruleKeys = (ArrayNode) root.get("ruleKeys");
    var keys = new HashSet<String>();
    var duplicatedKeys = new HashSet<String>();
    for (var ruleKey : ruleKeys) {
      if (!keys.add(ruleKey.asText())) {
        duplicatedKeys.add(ruleKey.asText());
      }
    }
    return duplicatedKeys;
  }
}
