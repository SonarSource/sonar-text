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
package org.sonar.plugins.secrets.configuration.model.matching.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PostModuleTest {

  private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory());

  @Test
  void shouldHaveDefaultConstructor() {
    var topLevelPostModule = new TopLevelPostModule();
    assertThat(topLevelPostModule).isNotNull();

    var namedPostModule = new NamedPostModule();
    assertThat(namedPostModule).isNotNull();
  }

  @Test
  void shouldDeserializePostModules() throws JsonProcessingException {
    var input = """
      patternNot:
        - example
      groups:
        - name: prefix
          patternNot:
            - ex
      """;

    var postModule = MAPPER.readValue(input, TopLevelPostModule.class);

    assertThat(postModule).isNotNull();
    assertThat(postModule.getPatternNot()).containsExactly("example");
    assertThat(postModule.getGroups()).hasSize(1);
    assertThat(postModule.getGroups().get(0).getName()).isEqualTo("prefix");
    assertThat(postModule.getGroups().get(0).getPatternNot()).containsExactly("ex");
  }

  @Test
  void shouldDeserializeAbsentFieldsOfTopLevelPostModuleToEmptyCollection() throws JsonProcessingException {
    var input = """
      statisticalFilter:
        threshold: 4
      """;
    var postModule = MAPPER.readValue(input, TopLevelPostModule.class);

    assertThat(postModule.getPatternNot()).isEmpty();
    assertThat(postModule.getHeuristicFilter()).isNull();
    assertThat(postModule.getStatisticalFilter().getThreshold()).isEqualTo(4);
    assertThat(postModule.getGroups()).isEmpty();
  }

}
