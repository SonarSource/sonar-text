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
package org.sonar.plugins.secrets;

import org.junit.jupiter.api.Test;
import org.sonar.api.SonarRuntime;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.plugins.common.AbstractRuleDefinitionTest;
import org.sonar.plugins.common.CommonRulesDefinition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class SecretsRulesDefinitionTest extends AbstractRuleDefinitionTest {

  @Override
  protected CommonRulesDefinition getRuleDefinition(SonarRuntime sonarRuntime) {
    return new SecretsRulesDefinition(sonarRuntime);
  }

  @Override
  protected BuiltInQualityProfilesDefinition getQualityProfile() {
    return new SecretsRulesDefinition.DefaultQualityProfile();
  }

  @Override
  protected String getRepositoryKey() {
    return SecretsRulesDefinition.REPOSITORY_KEY;
  }

  @Override
  protected String getRepositoryName() {
    return SecretsRulesDefinition.REPOSITORY_NAME;
  }

  @Override
  protected void customRepositoryAssertions(RulesDefinition.Repository repository, CommonRulesDefinition rulesDefinition) {
    assertThat(rulesDefinition.packagePrefix()).isEqualTo("org");

    RulesDefinition.Rule ruleS6290 = repository.rule("S6290");
    assertThat(ruleS6290).isNotNull();
    assertThat(ruleS6290.name()).isEqualTo("Amazon Web Services credentials should not be disclosed");
    assertThat(ruleS6290.activatedByDefault()).isTrue();
    assertThat(ruleS6290.type()).isEqualTo(RuleType.VULNERABILITY);
  }

  @Test
  void shouldCreateRepositoryInCommunityEdition() {
    var context = spy(new BuiltInQualityProfilesDefinition.Context());

    new SecretsRulesDefinition.DefaultQualityProfile()
      .define(context);

    verify(context, times(1)).createBuiltInQualityProfile(SecretsRulesDefinition.DefaultQualityProfile.NAME, SecretsLanguage.KEY);
  }
}
