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
import java.io.IOException;
import java.util.Iterator;
import org.sonar.plugins.secrets.configuration.model.matching.BooleanCombination;
import org.sonar.plugins.secrets.configuration.model.matching.Match;
import org.sonar.plugins.secrets.configuration.model.matching.AuxiliaryPattern;

public class MatchDeserializer extends JsonDeserializer<Match> {

  @Override
  public Match deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
    TreeNode treeNode = jsonParser.getCodec().readTree(jsonParser);

    JsonParser matchNodeParser = treeNode.traverse();
    matchNodeParser.setCodec(jsonParser.getCodec());

    Iterator<String> nodeIterator = treeNode.fieldNames();
    // As the yaml is validated before, there is always one element
    String name = nodeIterator.next();

    if (name.startsWith("pattern")) {
      return matchNodeParser.readValueAs(AuxiliaryPattern.class);
    } else {
      return matchNodeParser.readValueAs(BooleanCombination.class);
    }
  }
}
