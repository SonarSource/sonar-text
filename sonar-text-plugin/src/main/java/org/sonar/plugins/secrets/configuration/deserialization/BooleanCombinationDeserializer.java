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
package org.sonar.plugins.secrets.configuration.deserialization;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.sonar.plugins.secrets.configuration.model.matching.BooleanCombination;
import org.sonar.plugins.secrets.configuration.model.matching.BooleanCombinationType;
import org.sonar.plugins.secrets.configuration.model.matching.Match;

public class BooleanCombinationDeserializer extends JsonDeserializer<BooleanCombination> {

  public BooleanCombination deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
    TreeNode treeNode = jsonParser.getCodec().readTree(jsonParser);

    Iterator<Map.Entry<String, JsonNode>> properties = ((ObjectNode) treeNode).properties().iterator();
    // As the yaml is validated before, there is always one element!
    Map.Entry<String, JsonNode> node = properties.next();

    List<Match> modules = new ArrayList<>();

    if ("matchNot".equals(node.getKey())) {
      addMatch(jsonParser, node.getValue(), modules);
    } else {
      for (JsonNode matchNode : node.getValue()) {
        addMatch(jsonParser, matchNode, modules);
      }
    }

    BooleanCombination booleanCombination = new BooleanCombination();
    booleanCombination.setType(BooleanCombinationType.valueOfLabel(node.getKey()));
    booleanCombination.setMatches(modules);
    return booleanCombination;
  }

  private static void addMatch(JsonParser jsonParser, JsonNode matchNode, List<Match> modules) throws IOException {
    JsonParser matchNodeParser = matchNode.traverse();
    matchNodeParser.setCodec(jsonParser.getCodec());
    Match match = matchNodeParser.readValueAs(Match.class);
    modules.add(match);
  }
}
