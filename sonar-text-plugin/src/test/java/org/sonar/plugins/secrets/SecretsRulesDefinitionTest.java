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
package org.sonar.plugins.secrets;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.sonar.api.SonarRuntime;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.plugins.common.AbstractRuleDefinitionTest;
import org.sonar.plugins.common.CommonRulesDefinition;
import org.sonar.plugins.common.DefaultQualityProfileDefinition;
import org.sonar.plugins.common.TestUtils;
import org.sonarsource.analyzer.commons.BuiltInQualityProfileJsonLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class SecretsRulesDefinitionTest extends AbstractRuleDefinitionTest {

  /**
   * Package of the profile files simulating a commercial edition, living in {@code src/test/resources/testprofile}.
   */
  private static final String TEST_COMMERCIAL_PACKAGE_PREFIX = "testprofile";
  private static final String COMMERCIAL_RULE_KEY_1 = "S9001";
  private static final String COMMERCIAL_RULE_KEY_2 = "S9002";
  private static final String COMMERCIAL_SONARQUBE_CLOUD_ONLY_RULE_KEY = "S9003";

  @Override
  protected CommonRulesDefinition getRuleDefinition(SonarRuntime sonarRuntime) {
    return new SecretsRulesDefinition(sonarRuntime);
  }

  @Override
  protected BuiltInQualityProfilesDefinition getQualityProfile(SonarRuntime sonarRuntime) {
    return new SecretsRulesDefinition.DefaultQualityProfile(sonarRuntime);
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

    new SecretsRulesDefinition.DefaultQualityProfile(TestUtils.SONARQUBE_RUNTIME)
      .define(context);

    verify(context, times(1)).createBuiltInQualityProfile(SecretsRulesDefinition.DefaultQualityProfile.NAME, SecretsLanguage.KEY);
  }

  @Test
  void shouldOnlyLoadTheOpenSourceProfileInCommunityEdition() {
    var profile = defineProfile(new SecretsRulesDefinition.DefaultQualityProfile(TestUtils.SONARCLOUD_RUNTIME));

    assertThat(activeRuleKeys(profile))
      .containsAll(openSourceSonarWayRuleKeys())
      .doesNotContain(COMMERCIAL_RULE_KEY_1, COMMERCIAL_RULE_KEY_2, COMMERCIAL_SONARQUBE_CLOUD_ONLY_RULE_KEY);
  }

  @Test
  void shouldNotDeclareAnySonarQubeCloudOnlyProfileInCommunityEdition() {
    assertThat(new SecretsRulesDefinition.DefaultQualityProfile(TestUtils.SONARQUBE_RUNTIME).sonarQubeCloudOnlyProfilePaths()).isEmpty();
  }

  @Test
  void shouldLoadTheCommercialProfileInAdditionToTheOpenSourceOneInCommercialEdition() {
    var profile = defineProfile(new CommercialEditionQualityProfile(TestUtils.SONARQUBE_RUNTIME));

    assertThat(activeRuleKeys(profile))
      .containsAll(openSourceSonarWayRuleKeys())
      .contains(COMMERCIAL_RULE_KEY_1, COMMERCIAL_RULE_KEY_2)
      .doesNotContain(COMMERCIAL_SONARQUBE_CLOUD_ONLY_RULE_KEY);
  }

  @Test
  void shouldAlsoLoadTheSonarQubeCloudOnlyRulesInCommercialEditionOnSonarQubeCloud() {
    var profileDefinition = new CommercialEditionQualityProfile(TestUtils.SONARCLOUD_RUNTIME);
    assertThat(profileDefinition.sonarQubeCloudOnlyProfilePaths())
      .containsExactly("/" + TEST_COMMERCIAL_PACKAGE_PREFIX + "/sonar/l10n/secrets/rules/secrets/Sonar_way_sonarqube_cloud_addition_profile.json");

    var profile = defineProfile(profileDefinition);

    assertThat(activeRuleKeys(profile))
      .containsAll(openSourceSonarWayRuleKeys())
      .contains(COMMERCIAL_RULE_KEY_1, COMMERCIAL_RULE_KEY_2, COMMERCIAL_SONARQUBE_CLOUD_ONLY_RULE_KEY);
  }

  private static BuiltInQualityProfilesDefinition.BuiltInQualityProfile defineProfile(BuiltInQualityProfilesDefinition profileDefinition) {
    var context = new BuiltInQualityProfilesDefinition.Context();
    profileDefinition.define(context);
    return context.profile(SecretsLanguage.KEY, DefaultQualityProfileDefinition.NAME);
  }

  private static List<String> activeRuleKeys(BuiltInQualityProfilesDefinition.BuiltInQualityProfile profile) {
    return profile.rules().stream().map(BuiltInQualityProfilesDefinition.BuiltInActiveRule::ruleKey).toList();
  }

  private static Set<String> openSourceSonarWayRuleKeys() {
    return BuiltInQualityProfileJsonLoader.loadActiveKeysFromJsonProfile(
      DefaultQualityProfileDefinition.profilePath("org", SecretsRulesDefinition.REPOSITORY_KEY, SecretsLanguage.KEY));
  }

  /**
   * Simulates what the Developer and Enterprise edition plugins do: they load the profile of their own package in
   * addition to the open-source one. The profile files live in {@code src/test/resources/testprofile} instead of the
   * "com" package, which is not on the classpath of this module.
   */
  private static class CommercialEditionQualityProfile extends SecretsRulesDefinition.DefaultQualityProfile {
    CommercialEditionQualityProfile(SonarRuntime sonarRuntime) {
      super(sonarRuntime);
    }

    @Override
    public String packagePrefix() {
      return TEST_COMMERCIAL_PACKAGE_PREFIX;
    }
  }
}
