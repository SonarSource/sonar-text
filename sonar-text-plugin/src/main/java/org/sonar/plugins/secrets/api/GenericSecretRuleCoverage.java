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

import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import org.sonar.api.batch.rule.ActiveRule;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.plugins.secrets.SecretsRulesDefinition;
import org.sonar.plugins.text.TextRuleDefinition;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toUnmodifiableSet;

/** Immutable snapshot of active language equivalents for analyzer-generic rules, indexed by RSPEC key. */
public final class GenericSecretRuleCoverage {
  public static final GenericSecretRuleCoverage EMPTY = new GenericSecretRuleCoverage(Map.of());

  // Keep aligned with the rules whose specifications use selectivity: analyzerGeneric.
  static final Set<String> ANALYZER_GENERIC_RULE_KEYS = Set.of("S2068", "S6418");

  // Languages implementing S2068 on SQC: https://sonarcloud.io/api/rules/search?organization=sonarsource&q=S2068
  // Languages implementing S6418 on SQC: https://sonarcloud.io/api/rules/search?organization=sonarsource&q=S6418
  private final Map<String, Set<String>> languagesByRule;

  private GenericSecretRuleCoverage(Map<String, Set<String>> languagesByRule) {
    this.languagesByRule = languagesByRule;
  }

  /** Uses the rule's declared language, which can differ from its repository key. */
  public static GenericSecretRuleCoverage from(ActiveRules activeRules) {
    var languagesByRule = activeRules.findAll().stream()
      .filter(rule -> ANALYZER_GENERIC_RULE_KEYS.contains(rule.ruleKey().rule()))
      .filter(rule -> !SecretsRulesDefinition.REPOSITORY_KEY.equals(rule.ruleKey().repository()))
      .filter(rule -> !TextRuleDefinition.REPOSITORY_KEY.equals(rule.ruleKey().repository()))
      .filter(rule -> rule.language() != null && !rule.language().isBlank())
      .collect(collectingAndThen(groupingBy(rule -> rule.ruleKey().rule(), mapping(ActiveRule::language, toUnmodifiableSet())), Map::copyOf));
    return new GenericSecretRuleCoverage(languagesByRule);
  }

  /** Returns whether an equivalent rule is active for the assigned language; unassigned files are never covered. */
  public boolean isRuleActiveForLanguage(String rspecKey, @Nullable String language) {
    return language != null && languagesByRule.getOrDefault(rspecKey, Set.of()).contains(language);
  }
}
