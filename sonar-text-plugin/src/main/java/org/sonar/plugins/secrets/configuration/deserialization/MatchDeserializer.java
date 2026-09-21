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

import org.sonar.plugins.secrets.configuration.model.matching.AuxiliaryPattern;
import org.sonar.plugins.secrets.configuration.model.matching.BooleanCombination;
import org.sonar.plugins.secrets.configuration.model.matching.Match;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.node.ObjectNode;

public class MatchDeserializer extends ValueDeserializer<Match> {

  @Override
  public Match deserialize(JsonParser jsonParser, DeserializationContext ctxt) {
    JsonNode treeNode = ctxt.readTree(jsonParser);

    var nodeIterator = ((ObjectNode) treeNode).properties().iterator();
    // As the yaml is validated before, there is always one element
    String name = nodeIterator.next().getKey();

    if (name.startsWith("pattern")) {
      return ctxt.readTreeAsValue(treeNode, AuxiliaryPattern.class);
    } else {
      return ctxt.readTreeAsValue(treeNode, BooleanCombination.class);
    }
  }
}
