/*
 * SonarQube Text Plugin
 * Copyright (C) 2021-2024 SonarSource SA
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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import org.sonar.plugins.secrets.configuration.model.matching.AuxiliaryPattern;
import org.sonar.plugins.secrets.configuration.model.matching.AuxiliaryPatternType;

public class AuxiliaryPatternDeserializer extends JsonDeserializer<AuxiliaryPattern> {

  @Override
  public AuxiliaryPattern deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
    TreeNode treeNode = jsonParser.getCodec().readTree(jsonParser);

    Iterator<Map.Entry<String, JsonNode>> fields = ((ObjectNode) treeNode).fields();
    // As the yaml is validated before, there is always one element
    Map.Entry<String, JsonNode> node = fields.next();

    AuxiliaryPattern auxiliaryPattern = new AuxiliaryPattern();
    auxiliaryPattern.setType(AuxiliaryPatternType.valueOfLabel(node.getKey()));
    if (node.getValue() instanceof TextNode) {
      auxiliaryPattern.setPattern(node.getValue().asText());
    } else {
      JsonNode value = node.getValue();
      auxiliaryPattern.setPattern(value.get("pattern").asText());
      auxiliaryPattern.setMaxLineDistance(getOrNull(value, "maxLineDistance"));
      auxiliaryPattern.setMaxCharacterDistance(getOrNull(value, "maxCharDistance"));
    }
    return auxiliaryPattern;
  }

  private static Integer getOrNull(JsonNode node, String fieldName) {
    JsonNode childNode = node.get(fieldName);
    if (childNode != null) {
      return childNode.asInt();
    }
    return null;
  }
}
