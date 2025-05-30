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
package org.sonar.plugins.common;

import org.junit.jupiter.api.Test;
import org.sonar.api.SonarRuntime;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition;
import org.sonar.api.server.rule.RulesDefinition;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class AbstractRuleDefinitionTest {

  @Test
  void shouldDefineRules() {
    CommonRulesDefinition rulesDefinition = getRuleDefinition(TestUtils.SONARLINT_RUNTIME);
    RulesDefinition.Context context = new RulesDefinition.Context();
    rulesDefinition.define(context);

    assertThat(context.repositories()).hasSize(1);
    RulesDefinition.Repository repository = context.repository(getRepositoryKey());
    assertThat(repository).isNotNull();
    assertThat(repository.rules()).hasSize(rulesDefinition.checks().size());
    assertThat(repository.name()).isEqualTo(getRepositoryName());

    customRepositoryAssertions(repository, rulesDefinition);
  }

  @Test
  void shouldDefineSonarWayProfile() {
    BuiltInQualityProfilesDefinition.Context context = new BuiltInQualityProfilesDefinition.Context();
    BuiltInQualityProfilesDefinition profileDefinition = getQualityProfile();
    profileDefinition.define(context);
    BuiltInQualityProfilesDefinition.BuiltInQualityProfile profile = context.profile(getRepositoryKey(), "Sonar way");
    assertThat(profile.language()).isEqualTo(getRepositoryKey());
    assertThat(profile.name()).isEqualTo("Sonar way");
    assertThat(profile.rules()).hasSize(expectedSonarWayChecksCount(TestUtils.SONARLINT_RUNTIME));
  }

  protected abstract CommonRulesDefinition getRuleDefinition(SonarRuntime sonarRuntime);

  protected abstract BuiltInQualityProfilesDefinition getQualityProfile();

  protected abstract String getRepositoryKey();

  protected abstract String getRepositoryName();

  protected int expectedSonarWayChecksCount(SonarRuntime sonarRuntime) {
    var rulesDefinition = getRuleDefinition(sonarRuntime);
    return rulesDefinition.checks().size() - nonSonarWayRulesCount();
  }

  protected int nonSonarWayRulesCount() {
    return 0;
  }

  protected void customRepositoryAssertions(RulesDefinition.Repository repository, CommonRulesDefinition rulesDefinition) {
    // No custom assertions by default
  }
}
