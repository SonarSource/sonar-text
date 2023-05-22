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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import org.sonar.plugins.secrets.configuration.model.matching.PatternMatch;
import org.sonar.plugins.secrets.configuration.model.matching.PatternType;

public class PatternMatchDeserializer extends JsonDeserializer<PatternMatch> {

  @Override
  public PatternMatch deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
    TreeNode treeNode = jsonParser.getCodec().readTree(jsonParser);

    Iterator<Map.Entry<String, JsonNode>> fields = ((ObjectNode) treeNode).fields();
    // As the yaml is validated before, there is always one element
    Map.Entry<String, JsonNode> node = fields.next();

    PatternMatch patternMatch = new PatternMatch();
    patternMatch.setType(PatternType.valueOfLabel(node.getKey()));
    patternMatch.setPattern(node.getValue().asText());
    return patternMatch;
  }
}
