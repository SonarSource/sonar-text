/*
 * SonarQube Text Plugin
 * Copyright (C) 2021-2026 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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

class PreModuleTest {

  private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory());

  @Test
  void shouldDeserializeToEmptyCollection() throws JsonProcessingException {
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
  void shouldReturnOverrideWhenBaseIsNull() throws JsonProcessingException {
    var override = constructPreModule("""
      reject:
        paths:
          - path1
      """);

    var result = PreModule.merge(null, override);

    assertThat(result).isSameAs(override);
  }

  @Test
  void shouldReturnBaseWhenOverrideIsNull() throws JsonProcessingException {
    var base = constructPreModule("""
      reject:
        ext:
          - txt
      """);

    var result = PreModule.merge(base, null);

    assertThat(result).isSameAs(base);
  }

  @Test
  void shouldMergeBothIncludeAndRejectFilters() throws JsonProcessingException {
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

  private static PreModule constructPreModule(String spec) throws JsonProcessingException {
    return MAPPER.readValue(spec, PreModule.class);
  }
}
