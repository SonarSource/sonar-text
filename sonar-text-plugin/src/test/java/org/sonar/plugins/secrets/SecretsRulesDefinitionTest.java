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

  private static final SonarRuntime sonarRuntime = SonarRuntimeImpl.forSonarLint(Version.create(8, 9));

  @Test
  void shouldDefineRules() {
    SecretsRulesDefinition rulesDefinition = new SecretsRulesDefinition(sonarRuntime);
    RulesDefinition.Context context = new RulesDefinition.Context();
    rulesDefinition.define(context);

    assertThat(context.repositories()).hasSize(1);
    RulesDefinition.Repository repository = context.repository(REPOSITORY_KEY);
    assertThat(repository).isNotNull();
    assertThat(repository.rules()).hasSize(rulesDefinition.checks().size());
    assertThat(repository.name()).isEqualTo("Sonar Secrets Analyzer");

    RulesDefinition.Rule ruleS6290 = repository.rule("S6290");
    assertThat(ruleS6290).isNotNull();
    assertThat(ruleS6290.name()).isEqualTo("Amazon Web Services credentials should not be disclosed");
    assertThat(ruleS6290.activatedByDefault()).isTrue();
    assertThat(ruleS6290.type()).isEqualTo(RuleType.VULNERABILITY);

    assertThat(rulesDefinition.packagePrefix()).isEqualTo("org");
  }

  @Test
  void shouldDefineSonarWayProfile() {
    SecretsRulesDefinition rulesDefinition = new SecretsRulesDefinition(sonarRuntime);
    BuiltInQualityProfilesDefinition.Context context = new BuiltInQualityProfilesDefinition.Context();
    BuiltInQualityProfilesDefinition profileDefinition = new SecretsRulesDefinition.DefaultQualityProfile();
    profileDefinition.define(context);
    BuiltInQualityProfilesDefinition.BuiltInQualityProfile profile = context.profile("secrets", "Sonar way");
    assertThat(profile.language()).isEqualTo("secrets");
    assertThat(profile.name()).isEqualTo("Sonar way");
    assertThat(profile.rules()).hasSize(rulesDefinition.checks().size());
  }

  @Test
  void eachCheckShouldBeDeclaredInTheCheckList() throws IOException {
    SecretsRulesDefinition rulesDefinition = new SecretsRulesDefinition(sonarRuntime);
    Path checksPackage = Path.of("src", "main", "java", "org", "sonar", "plugins", "secrets", "checks");
    try (Stream<Path> list = Files.walk(checksPackage)) {
      int expectedCount = (int) list.filter(file -> file.toString().endsWith("Check.java")).count();
      assertThat(rulesDefinition.checks()).hasSize(expectedCount);
    }
  }
}
