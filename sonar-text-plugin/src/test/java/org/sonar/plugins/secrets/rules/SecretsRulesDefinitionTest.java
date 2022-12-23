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
package org.sonar.plugins.secrets.rules;

import org.junit.Test;
import org.sonar.api.internal.SonarRuntimeImpl;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.utils.Version;
import org.sonar.plugins.secrets.rules.SecretsRulesDefinition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.plugins.secrets.rules.SecretsRulesDefinition.REPOSITORY_KEY;

public class SecretsRulesDefinitionTest {

  SecretsRulesDefinition underTest = new SecretsRulesDefinition(SonarRuntimeImpl.forSonarLint(Version.create(8, 9)));

  @Test
  public void defineTest() {
    RulesDefinition.Context context = new RulesDefinition.Context();
    underTest.define(context);

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

}
