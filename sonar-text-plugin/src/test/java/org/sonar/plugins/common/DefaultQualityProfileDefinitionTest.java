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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.sonar.api.SonarRuntime;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.plugins.common.TestUtils.SONARCLOUD_RUNTIME;
import static org.sonar.plugins.common.TestUtils.SONARLINT_RUNTIME;
import static org.sonar.plugins.common.TestUtils.SONARQUBE_RUNTIME;

class DefaultQualityProfileDefinitionTest {

  /**
   * Package of the profile files used by this test, living in {@code src/test/resources/testprofile}. It plays the role
   * the "org" and "com" packages play in production, see {@link DefaultQualityProfileDefinition#packagePrefix()}.
   */
  private static final String TEST_PACKAGE_PREFIX = "testprofile";
  private static final String REPOSITORY_KEY = "secrets";
  private static final String LANGUAGE_KEY = "secrets";
  private static final String REGULAR_PROFILE_RULE_KEY_1 = "S9001";
  private static final String REGULAR_PROFILE_RULE_KEY_2 = "S9002";
  private static final String SONARQUBE_CLOUD_ONLY_RULE_KEY = "S9003";

  @Test
  void shouldActivateTheRulesOfTheRegularProfileAsDefaultProfile() {
    var profile = defineProfile(new TestProfileDefinition(SONARQUBE_RUNTIME));

    assertThat(profile.name()).isEqualTo(DefaultQualityProfileDefinition.NAME);
    assertThat(profile.language()).isEqualTo(LANGUAGE_KEY);
    assertThat(profile.isDefault()).isTrue();
    assertThat(activeRuleKeys(profile)).containsExactlyInAnyOrder(REGULAR_PROFILE_RULE_KEY_1, REGULAR_PROFILE_RULE_KEY_2);
  }

  @Test
  void shouldActivateSonarQubeCloudOnlyRulesOnSonarQubeCloud() {
    var profile = defineProfile(new TestProfileDefinitionWithSonarQubeCloudAddition(SONARCLOUD_RUNTIME));

    assertThat(activeRuleKeys(profile))
      .containsExactlyInAnyOrder(REGULAR_PROFILE_RULE_KEY_1, REGULAR_PROFILE_RULE_KEY_2, SONARQUBE_CLOUD_ONLY_RULE_KEY);
  }

  @ParameterizedTest
  @MethodSource("runtimesWithoutSonarQubeCloudRules")
  void shouldNotActivateSonarQubeCloudOnlyRulesOnOtherProducts(SonarRuntime sonarRuntime) {
    var profile = defineProfile(new TestProfileDefinitionWithSonarQubeCloudAddition(sonarRuntime));

    assertThat(activeRuleKeys(profile)).doesNotContain(SONARQUBE_CLOUD_ONLY_RULE_KEY);
  }

  static List<SonarRuntime> runtimesWithoutSonarQubeCloudRules() {
    return List.of(SONARQUBE_RUNTIME, SONARLINT_RUNTIME);
  }

  @Test
  void shouldNotActivateAnyAdditionalRuleOnSonarQubeCloudWhenNoSonarQubeCloudProfileIsDeclared() {
    var profileDefinition = new TestProfileDefinition(SONARCLOUD_RUNTIME);
    assertThat(profileDefinition.sonarQubeCloudOnlyProfilePaths()).isEmpty();

    var profile = defineProfile(profileDefinition);

    assertThat(activeRuleKeys(profile)).containsExactlyInAnyOrder(REGULAR_PROFILE_RULE_KEY_1, REGULAR_PROFILE_RULE_KEY_2);
  }

  @Test
  void shouldBuildTheResourcePathsOfTheProfileFiles() {
    assertThat(DefaultQualityProfileDefinition.profilePath("org", "secrets", "secrets"))
      .isEqualTo("/org/sonar/l10n/secrets/rules/secrets/Sonar_way_profile.json");
    assertThat(DefaultQualityProfileDefinition.sonarQubeCloudProfilePath("com", "secrets", "secrets"))
      .isEqualTo("/com/sonar/l10n/secrets/rules/secrets/Sonar_way_sonarqube_cloud_addition_profile.json");
  }

  @Test
  void shouldUseTheOpenSourcePackageByDefault() {
    assertThat(new DefaultQualityProfileDefinition(SONARQUBE_RUNTIME, REPOSITORY_KEY, LANGUAGE_KEY).packagePrefix()).isEqualTo("org");
  }

  private static BuiltInQualityProfilesDefinition.BuiltInQualityProfile defineProfile(DefaultQualityProfileDefinition profileDefinition) {
    var context = new BuiltInQualityProfilesDefinition.Context();
    profileDefinition.define(context);
    return context.profile(LANGUAGE_KEY, DefaultQualityProfileDefinition.NAME);
  }

  private static List<String> activeRuleKeys(BuiltInQualityProfilesDefinition.BuiltInQualityProfile profile) {
    return profile.rules().stream().map(BuiltInQualityProfilesDefinition.BuiltInActiveRule::ruleKey).toList();
  }

  /**
   * Loads its profile from the test resources instead of the production ones, like the commercial plugins load theirs
   * from the "com" package.
   */
  private static class TestProfileDefinition extends DefaultQualityProfileDefinition {
    TestProfileDefinition(SonarRuntime sonarRuntime) {
      super(sonarRuntime, REPOSITORY_KEY, LANGUAGE_KEY);
    }

    @Override
    public String packagePrefix() {
      return TEST_PACKAGE_PREFIX;
    }
  }

  private static class TestProfileDefinitionWithSonarQubeCloudAddition extends TestProfileDefinition {
    TestProfileDefinitionWithSonarQubeCloudAddition(SonarRuntime sonarRuntime) {
      super(sonarRuntime);
    }

    @Override
    protected List<String> sonarQubeCloudOnlyProfilePaths() {
      return List.of(sonarQubeCloudProfilePath(packagePrefix(), repositoryKey, languageKey));
    }
  }
}
