/*
 * SonarQube Text Plugin
 * Copyright (C) 2021-2022 SonarSource SA
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
package org.sonar.plugins.secrets;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.sonar.api.SonarRuntime;
import org.sonar.api.internal.SonarRuntimeImpl;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.utils.Version;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.plugins.secrets.SecretsRulesDefinition.REPOSITORY_KEY;

class SecretsRulesDefinitionTest {

  @Test
  void define_rules() {
    SonarRuntime sonarRuntime = SonarRuntimeImpl.forSonarLint(Version.create(8, 9));
    SecretsRulesDefinition rulesDefinition = new SecretsRulesDefinition(sonarRuntime);
    RulesDefinition.Context context = new RulesDefinition.Context();
    rulesDefinition.define(context);

    assertThat(context.repositories()).hasSize(1);
    RulesDefinition.Repository repository = context.repository(REPOSITORY_KEY);
    assertThat(repository).isNotNull();
    assertThat(repository.rules()).hasSize(7);
    assertThat(repository.name()).isEqualTo("Sonar Secrets Analyzer");

    RulesDefinition.Rule ruleS7529 = repository.rule("S6290");
    assertThat(ruleS7529).isNotNull();
    assertThat(ruleS7529.name()).isEqualTo("Amazon Web Services credentials should not be disclosed");
    assertThat(ruleS7529.activatedByDefault()).isTrue();
    assertThat(ruleS7529.htmlDescription()).contains("AWS credentials are designed to authenticate and authorize requests to AWS.");
    assertThat(ruleS7529.type()).isEqualTo(RuleType.VULNERABILITY);
  }

  @Test
  void define_sonar_way_profile() {
    BuiltInQualityProfilesDefinition.Context context = new BuiltInQualityProfilesDefinition.Context();
    BuiltInQualityProfilesDefinition profileDefinition = new SecretsRulesDefinition.DefaultQualityProfile();
    profileDefinition.define(context);
    BuiltInQualityProfilesDefinition.BuiltInQualityProfile profile = context.profile("secrets", "Sonar way");
    assertThat(profile.language()).isEqualTo("secrets");
    assertThat(profile.name()).isEqualTo("Sonar way");
    assertThat(profile.rules()).hasSize(7);
  }

  @Test
  void each_check_should_be_declared_in_the_check_list() throws IOException {
    Path checksPackage = Path.of("src","main","java","org","sonar","plugins","secrets","checks");
    try (Stream<Path> list = Files.list(checksPackage)) {
      int expectedCount = (int) list.filter(file -> file.toString().endsWith("Check.java")).count();
      assertThat(SecretsRulesDefinition.checks()).hasSize(expectedCount);
    }
  }

}
