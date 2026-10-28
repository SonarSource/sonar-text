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
package org.sonar.plugins.secrets.api;

import org.junit.jupiter.api.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.scanner.plugin.api.impl.rule.ActiveRulesBuilder;
import org.sonar.scanner.plugin.api.impl.rule.NewActiveRule;

import static org.assertj.core.api.Assertions.assertThat;

class GenericSecretRuleCoverageTest {
  @Test
  void shouldUseDeclaredLanguageAndKeepRulesIndependent() {
    var builder = new ActiveRulesBuilder();
    builder.addRule(new NewActiveRule.Builder().setRuleKey(RuleKey.of("csharpsquid", "S2068")).setLanguage("cs").build());
    builder.addRule(new NewActiveRule.Builder().setRuleKey(RuleKey.of("java", "S6418")).setLanguage("java").build());
    builder.addRule(new NewActiveRule.Builder().setRuleKey(RuleKey.of("java", "S100")).setLanguage("java").build());
    builder.addRule(new NewActiveRule.Builder().setRuleKey(RuleKey.of("java", "S6437")).setLanguage("java").build());
    var coverage = GenericSecretRuleCoverage.from(builder.build());

    assertThat(coverage.isRuleActiveForLanguage("S2068", "cs")).isTrue();
    assertThat(coverage.isRuleActiveForLanguage("S6418", "java")).isTrue();
    assertThat(coverage.isRuleActiveForLanguage("S100", "java")).isFalse();
    assertThat(coverage.isRuleActiveForLanguage("S6437", "java")).isFalse();
    assertThat(coverage.isRuleActiveForLanguage("S2068", "csharpsquid")).isFalse();
    assertThat(coverage.isRuleActiveForLanguage("S6418", "cs")).isFalse();
    assertThat(coverage.isRuleActiveForLanguage("S2068", "java")).isFalse();
    assertThat(coverage.isRuleActiveForLanguage("S2068", "r")).isFalse();
    assertThat(coverage.isRuleActiveForLanguage("S2068", null)).isFalse();
  }

  @Test
  void shouldExcludeOwnRepositoriesAndRulesWithoutLanguages() {
    var builder = new ActiveRulesBuilder();
    builder.addRule(new NewActiveRule.Builder().setRuleKey(RuleKey.of("secrets", "S2068")).setLanguage("r").build());
    builder.addRule(new NewActiveRule.Builder().setRuleKey(RuleKey.of("text", "S6418")).setLanguage("r").build());
    builder.addRule(new NewActiveRule.Builder().setRuleKey(RuleKey.of("unknown", "S2068")).build());
    builder.addRule(new NewActiveRule.Builder().setRuleKey(RuleKey.of("blank", "S6418")).setLanguage(" ").build());
    var coverage = GenericSecretRuleCoverage.from(builder.build());

    assertThat(coverage.isRuleActiveForLanguage("S2068", "r")).isFalse();
    assertThat(coverage.isRuleActiveForLanguage("S6418", "r")).isFalse();
    assertThat(coverage.isRuleActiveForLanguage("S2068", null)).isFalse();
    assertThat(coverage.isRuleActiveForLanguage("S6418", " ")).isFalse();
  }

  @Test
  void shouldIncludeEveryLanguageWithTheSameActiveRule() {
    var builder = new ActiveRulesBuilder();
    builder.addRule(new NewActiveRule.Builder().setRuleKey(RuleKey.of("java", "S2068")).setLanguage("java").build());
    builder.addRule(new NewActiveRule.Builder().setRuleKey(RuleKey.of("csharpsquid", "S2068")).setLanguage("cs").build());
    var coverage = GenericSecretRuleCoverage.from(builder.build());

    assertThat(coverage.isRuleActiveForLanguage("S2068", "java")).isTrue();
    assertThat(coverage.isRuleActiveForLanguage("S2068", "cs")).isTrue();
  }
}
