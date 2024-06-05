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

class PreModuleTest {

  private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory());

  @Test
  void shouldDeserializeToEmptyCollection() throws JsonProcessingException {
    String input = """
      include:
        content: []
        paths: null
      """;
    PreModule preModule = MAPPER.readValue(input, PreModule.class);

    assertThat(preModule.getReject()).isNull();
    assertThat(preModule.getInclude()).isNotNull();
    assertThat(preModule.getScopes()).isEmpty();

    assertThat(preModule.getInclude().getContent()).isEmpty();
    assertThat(preModule.getInclude().getPaths()).isEmpty();
    assertThat(preModule.getInclude().getExt()).isEmpty();
  }
}
