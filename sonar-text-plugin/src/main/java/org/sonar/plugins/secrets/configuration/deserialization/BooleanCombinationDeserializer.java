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
package org.sonar.plugins.secrets.configuration.deserialization;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.sonar.plugins.secrets.configuration.model.matching.BooleanCombination;
import org.sonar.plugins.secrets.configuration.model.matching.BooleanCombinationType;
import org.sonar.plugins.secrets.configuration.model.matching.Match;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.node.ObjectNode;

public class BooleanCombinationDeserializer extends ValueDeserializer<BooleanCombination> {

  @Override
  public BooleanCombination deserialize(JsonParser jsonParser, DeserializationContext ctxt) {
    JsonNode treeNode = ctxt.readTree(jsonParser);

    Iterator<Map.Entry<String, JsonNode>> properties = ((ObjectNode) treeNode).properties().iterator();
    // As the yaml is validated before, there is always one element!
    Map.Entry<String, JsonNode> node = properties.next();

    List<Match> modules = new ArrayList<>();

    if ("matchNot".equals(node.getKey())) {
      addMatch(ctxt, node.getValue(), modules);
    } else {
      for (JsonNode matchNode : node.getValue()) {
        addMatch(ctxt, matchNode, modules);
      }
    }

    BooleanCombination booleanCombination = new BooleanCombination();
    booleanCombination.setType(BooleanCombinationType.valueOfLabel(node.getKey()));
    booleanCombination.setMatches(modules);
    return booleanCombination;
  }

  private static void addMatch(DeserializationContext ctxt, JsonNode matchNode, List<Match> modules) {
    Match match = ctxt.readTreeAsValue(matchNode, Match.class);
    modules.add(match);
  }
}
