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
package org.sonar.plugins.secrets.configuration.model.matching.filter;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.yaml.YAMLMapper;

import static org.assertj.core.api.Assertions.assertThat;

class PostModuleTest {

  private static final ObjectMapper MAPPER = YAMLMapper.builder()
    .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    .build();

  @Test
  void shouldHaveDefaultConstructor() {
    var topLevelPostModule = new TopLevelPostModule();
    assertThat(topLevelPostModule).isNotNull();

    var namedPostModule = new NamedPostModule();
    assertThat(namedPostModule).isNotNull();
  }

  @Test
  void shouldDeserializePostModules() {
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
  void shouldDeserializeAbsentFieldsOfTopLevelPostModuleToEmptyCollection() {
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
