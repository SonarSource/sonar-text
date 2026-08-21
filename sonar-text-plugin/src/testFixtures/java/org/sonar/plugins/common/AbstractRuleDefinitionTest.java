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
package org.sonar.plugins.common;

import java.util.List;
import java.util.Set;
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
    BuiltInQualityProfilesDefinition.BuiltInQualityProfile profile = defineSonarWayProfile(TestUtils.SONARLINT_RUNTIME);
    assertThat(profile.language()).isEqualTo(getRepositoryKey());
    assertThat(profile.name()).isEqualTo("Sonar way");
    assertThat(profile.rules()).hasSize(expectedSonarWayChecksCount(TestUtils.SONARLINT_RUNTIME));
    assertThat(activeRuleKeys(profile))
      .as("Rules rolled out on SonarQube Cloud only must not be in \"Sonar way\" on the other products")
      .noneMatch(sonarQubeCloudOnlyRuleKeys()::contains);
  }

  @Test
  void shouldDefineSonarWayProfileWithoutSonarQubeCloudOnlyRulesOnSonarQubeServer() {
    BuiltInQualityProfilesDefinition.BuiltInQualityProfile profile = defineSonarWayProfile(TestUtils.SONARQUBE_RUNTIME);
    assertThat(profile.rules()).hasSize(expectedSonarWayChecksCount(TestUtils.SONARQUBE_RUNTIME));
    assertThat(activeRuleKeys(profile)).noneMatch(sonarQubeCloudOnlyRuleKeys()::contains);
  }

  @Test
  void shouldDefineSonarWayProfileWithAdditionalRulesOnSonarQubeCloud() {
    BuiltInQualityProfilesDefinition.BuiltInQualityProfile profile = defineSonarWayProfile(TestUtils.SONARCLOUD_RUNTIME);
    assertThat(profile.rules()).hasSize(expectedSonarWayChecksCount(TestUtils.SONARCLOUD_RUNTIME) + sonarQubeCloudOnlyRuleKeys().size());
    assertThat(activeRuleKeys(profile)).containsAll(sonarQubeCloudOnlyRuleKeys());
  }

  private BuiltInQualityProfilesDefinition.BuiltInQualityProfile defineSonarWayProfile(SonarRuntime sonarRuntime) {
    BuiltInQualityProfilesDefinition.Context context = new BuiltInQualityProfilesDefinition.Context();
    getQualityProfile(sonarRuntime).define(context);
    return context.profile(getRepositoryKey(), "Sonar way");
  }

  private static List<String> activeRuleKeys(BuiltInQualityProfilesDefinition.BuiltInQualityProfile profile) {
    return profile.rules().stream().map(BuiltInQualityProfilesDefinition.BuiltInActiveRule::ruleKey).toList();
  }

  protected abstract CommonRulesDefinition getRuleDefinition(SonarRuntime sonarRuntime);

  protected abstract BuiltInQualityProfilesDefinition getQualityProfile(SonarRuntime sonarRuntime);

  /**
   * Keys of the rules that belong to "Sonar way" on SonarQube Cloud only, because they are being rolled out gradually
   * there. They are expected to be absent from {@link org.sonar.plugins.common.DefaultQualityProfileDefinition#FILE_NAME}
   * and present in {@link org.sonar.plugins.common.DefaultQualityProfileDefinition#SONARQUBE_CLOUD_FILE_NAME}.
   */
  protected Set<String> sonarQubeCloudOnlyRuleKeys() {
    return Set.of();
  }

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
