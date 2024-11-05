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
