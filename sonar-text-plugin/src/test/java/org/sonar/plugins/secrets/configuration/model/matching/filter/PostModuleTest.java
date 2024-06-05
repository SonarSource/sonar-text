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
package org.sonar.plugins.secrets.configuration.model.matching.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PostModuleTest {

  private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory());

  @Test
  void shouldDeserializeToEmptyCollection() throws JsonProcessingException {
    String input = """
      statisticalFilter:
        threshold: 4
      """;
    PostModule postModule = MAPPER.readValue(input, PostModule.class);

    assertThat(postModule.getPatternNot()).isEmpty();
    assertThat(postModule.getHeuristicFilter()).isNull();
    assertThat(postModule.getStatisticalFilter().getThreshold()).isEqualTo(4);
  }

}
