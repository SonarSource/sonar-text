/*
 * SonarQube Text Plugin
 * Copyright (C) 2021-2023 SonarSource SA
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
package org.sonar.plugins.text;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.sonar.api.SonarRuntime;
import org.sonar.api.internal.SonarRuntimeImpl;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.utils.Version;

import static org.assertj.core.api.Assertions.assertThat;

class TextRuleDefinitionTest {

  @Test
  void define_rules() {
    SonarRuntime sonarRuntime = SonarRuntimeImpl.forSonarLint(Version.parse("7.2.1.58118"));
    TextRuleDefinition rulesDefinition = new TextRuleDefinition(sonarRuntime);
    RulesDefinition.Context context = new RulesDefinition.Context();
    rulesDefinition.define(context);

    RulesDefinition.Repository repository = context.repository("text");
    assertThat(repository).isNotNull();
    assertThat(repository.name()).isEqualTo("Sonar");
    assertThat(repository.language()).isEqualTo("text");
    assertThat(repository.rules()).hasSize(1);
  }

  @Test
  void define_sonar_way_profile() {
    BuiltInQualityProfilesDefinition.Context context = new BuiltInQualityProfilesDefinition.Context();
    BuiltInQualityProfilesDefinition profileDefinition = new TextRuleDefinition.DefaultQualityProfile();
    profileDefinition.define(context);
    BuiltInQualityProfilesDefinition.BuiltInQualityProfile profile = context.profile("text", "Sonar way");
    assertThat(profile.language()).isEqualTo("text");
    assertThat(profile.name()).isEqualTo("Sonar way");
    assertThat(profile.rules()).hasSize(1);
  }

  @Test
  void each_check_should_be_declared_in_the_check_list() throws IOException {
    Path checksPackage = Path.of("src", "main", "java", "org", "sonar", "plugins", "text", "checks");
    try (Stream<Path> list = Files.list(checksPackage)) {
      int expectedCount = (int) list.filter(file -> file.toString().endsWith("Check.java")).count();
      assertThat(TextRuleDefinition.checks()).hasSize(expectedCount);
    }
  }

}
