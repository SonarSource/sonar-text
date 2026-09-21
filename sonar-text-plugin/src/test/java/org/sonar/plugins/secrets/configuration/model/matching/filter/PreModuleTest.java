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

class PreModuleTest {

  private static final ObjectMapper MAPPER = YAMLMapper.builder()
    .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    .build();

  @Test
  void shouldDeserializeToEmptyCollection() {
    var input = """
      include:
        content: []
        paths: null
      """;
    var preModule = constructPreModule(input);

    assertThat(preModule.getReject()).isNull();
    assertThat(preModule.getInclude()).isNotNull();

    assertThat(preModule.getInclude().getContent()).isEmpty();
    assertThat(preModule.getInclude().getPaths()).isEmpty();
    assertThat(preModule.getInclude().getExt()).isEmpty();
  }

  @Test
  void shouldMergeWhenBothAreNull() {
    var result = PreModule.merge(null, null);
    assertThat(result).isNull();
  }

  @Test
  void shouldReturnOverrideWhenBaseIsNull() {
    var override = constructPreModule("""
      reject:
        paths:
          - path1
      """);

    var result = PreModule.merge(null, override);

    assertThat(result).isSameAs(override);
  }

  @Test
  void shouldReturnBaseWhenOverrideIsNull() {
    var base = constructPreModule("""
      reject:
        ext:
          - txt
      """);

    var result = PreModule.merge(base, null);

    assertThat(result).isSameAs(base);
  }

  @Test
  void shouldMergeBothIncludeAndRejectFilters() {
    var base = MAPPER.readValue("""
      include:
        paths:
          - path1
        ext:
          - txt
      reject:
        content:
          - not-relevant
      """, PreModule.class);

    var override = MAPPER.readValue("""
      include:
        content:
          - test
        paths:
          - path2
      reject:
        ext:
          - log
      """, PreModule.class);

    var result = PreModule.merge(base, override);

    assertThat(result.getInclude()).isNotNull();
    assertThat(result.getInclude().getPaths()).containsExactly("path1", "path2");
    assertThat(result.getInclude().getExt()).containsExactly("txt");
    assertThat(result.getInclude().getContent()).containsExactly("test");

    assertThat(result.getReject()).isNotNull();
    assertThat(result.getReject().getPaths()).isEmpty();
    assertThat(result.getReject().getContent()).containsExactly("not-relevant");
    assertThat(result.getReject().getExt()).containsExactly("log");
  }

  @Test
  void shouldMergeEmptyPreModules() {
    var base = new PreModule();
    var override = new PreModule();

    var result = PreModule.merge(base, override);

    assertThat(result).isNotNull();
    assertThat(result.getInclude()).isNull();
    assertThat(result.getReject()).isNull();
  }

  private static PreModule constructPreModule(String spec) {
    return MAPPER.readValue(spec, PreModule.class);
  }
}
