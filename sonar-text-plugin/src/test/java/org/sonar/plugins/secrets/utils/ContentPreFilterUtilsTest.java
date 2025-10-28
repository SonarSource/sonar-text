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
package org.sonar.plugins.secrets.utils;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sonar.api.rule.RuleKey;
import org.sonar.plugins.common.Check;
import org.sonar.plugins.secrets.api.SecretsSpecificationLoader;
import org.sonar.plugins.secrets.configuration.deserialization.SpecificationDeserializer;
import org.sonar.plugins.secrets.configuration.model.Specification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.sonar.plugins.secrets.utils.SmileConverter.convertYamlToSmileStream;

class ContentPreFilterUtilsTest {

  @Mock
  private Check check1;
  @Mock
  private Check check2;
  @Mock
  private Check check3;
  @Mock
  private RuleKey ruleKey1;
  @Mock
  private RuleKey ruleKey2;
  @Mock
  private RuleKey ruleKey3;
  @Mock
  private SecretsSpecificationLoader specLoader;
  private AutoCloseable mocks;

  // language=YAML
  private final Specification spec = SpecificationDeserializer.deserialize(convertYamlToSmileStream("""
    provider:
      rules:
        - id: rule1
          # rule1 has valid prefilters
          rspecKey: rule1
          detection:
            pre:
              include:
                content: [password]
        - id: rule2
          # rule2 has no prefilters
          rspecKey: rule2
          detection:
            matching:
        - id: rule3
          # rule3 has empty prefilters
          rspecKey: rule3
          detection:
            pre:
              include:
                content: []
    """), "test.yaml");

  @BeforeEach
  void setUp() {
    mocks = MockitoAnnotations.openMocks(this);
  }

  @AfterEach
  void tearDown() throws Exception {
    mocks.close();
  }

  @Test
  void shouldOnlyIncludeValidOnesWhenGroupingByPrefilterWithMixedPreFilterAvailability() {
    var rule1 = spec.getProvider().getRules().get(0);
    var rule2 = spec.getProvider().getRules().get(1);
    var rule3 = spec.getProvider().getRules().get(2);

    when(check1.getRuleKey()).thenReturn(ruleKey1);
    when(check2.getRuleKey()).thenReturn(ruleKey2);
    when(check3.getRuleKey()).thenReturn(ruleKey3);
    when(ruleKey1.rule()).thenReturn("rule1");
    when(ruleKey2.rule()).thenReturn("rule2");
    when(ruleKey3.rule()).thenReturn("rule3");
    when(specLoader.getRulesForKey("rule1")).thenReturn(List.of(rule1));
    when(specLoader.getRulesForKey("rule2")).thenReturn(List.of(rule2));
    when(specLoader.getRulesForKey("rule3")).thenReturn(List.of(rule3));

    var checks = List.of(check1, check2, check3);

    var result = ContentPreFilterUtils.getChecksByContentPreFilters(checks, specLoader);

    assertThat(result).hasSize(1);
    assertThat(result.get("password")).containsExactly(check1);
  }

  @Test
  void shouldCreateTrieCorrectly() {
    var checksByPreFilters = new HashMap<String, Collection<Check>>();
    checksByPreFilters.put("Password", Set.of(check1));
    checksByPreFilters.put("TOKEN", Set.of(check2));
    checksByPreFilters.put("secret", Set.of(check1, check2));

    var trie = ContentPreFilterUtils.getPreprocessedTrie(checksByPreFilters);

    assertThat(trie).isNotNull();

    var emits1 = trie.parseText("My Password is secret");
    assertThat(emits1)
      .hasSize(2);

    var passwordEmit = emits1.stream()
      .filter(emit -> emit.getKeyword().equals("password"))
      .findFirst();
    assertThat(passwordEmit).isPresent();
    assertThat(passwordEmit.get().getPayload()).containsExactly(check1);

    var secretEmit = emits1.stream()
      .filter(emit -> emit.getKeyword().equals("secret"))
      .findFirst();
    assertThat(secretEmit).isPresent();
    assertThat(secretEmit.get().getPayload()).containsExactlyInAnyOrder(check1, check2);

    // case-insensitive matching
    var emits2 = trie.parseText("API token value");
    assertThat(emits2).hasSize(1);
    var tokenEmit = emits2.iterator().next();
    assertThat(tokenEmit.getKeyword()).isEqualTo("token");
    assertThat(tokenEmit.getPayload()).containsExactly(check2);
  }

  @Test
  void shouldCreateEmptyTrieWithEmptyInput() {
    var checksByPreFilters = new HashMap<String, Collection<Check>>();

    var trie = ContentPreFilterUtils.getPreprocessedTrie(checksByPreFilters);
    assertThat(trie).isNotNull();

    var emits = trie.parseText("password secret token");
    assertThat(emits).isEmpty();
  }

  @Test
  void shouldHandleOverlappingMatches() {
    var checksByPreFilters = new HashMap<String, Collection<Check>>();
    checksByPreFilters.put("pass", Set.of(check1));
    checksByPreFilters.put("password", Set.of(check2));
    checksByPreFilters.put("sword", Set.of(check3));

    var trie = ContentPreFilterUtils.getPreprocessedTrie(checksByPreFilters);

    var emits = trie.parseText("password");
    assertThat(emits).hasSize(3); // "pass", "password", and "sword" should all match

    var resultMap = new HashMap<String, Collection<Check>>();
    for (var emit : emits) {
      resultMap.put(emit.getKeyword(), emit.getPayload());
    }

    assertThat(resultMap.get("pass")).containsExactly(check1);
    assertThat(resultMap.get("password")).containsExactly(check2);
    assertThat(resultMap.get("sword")).containsExactly(check3);
  }
}
