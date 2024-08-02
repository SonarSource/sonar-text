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
package org.sonar.plugins.text;

import org.sonar.api.SonarRuntime;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.plugins.common.AbstractRuleDefinitionTest;
import org.sonar.plugins.common.CommonRulesDefinition;

import static org.assertj.core.api.Assertions.assertThat;

class TextRuleDefinitionTest extends AbstractRuleDefinitionTest {

  @Override
  protected CommonRulesDefinition getRuleDefinition(SonarRuntime sonarRuntime) {
    return new TextRuleDefinition(sonarRuntime);
  }

  @Override
  protected BuiltInQualityProfilesDefinition getQualityProfile() {
    return new TextRuleDefinition.DefaultQualityProfile();
  }

  @Override
  protected String getRepositoryKey() {
    return TextRuleDefinition.REPOSITORY_KEY;
  }

  @Override
  protected String getRepositoryName() {
    return TextRuleDefinition.REPOSITORY_NAME;
  }

  @Override
  protected void customRepositoryAssertions(RulesDefinition.Repository repository, CommonRulesDefinition rulesDefinition) {
    assertThat(rulesDefinition.packagePrefix()).isEqualTo("org");

    RulesDefinition.Rule ruleS6389 = repository.rule("S6389");
    assertThat(ruleS6389).isNotNull();
    assertThat(ruleS6389.name()).isEqualTo("Using bidirectional characters is security-sensitive");
    assertThat(ruleS6389.activatedByDefault()).isTrue();
    assertThat(ruleS6389.type()).isEqualTo(RuleType.SECURITY_HOTSPOT);
  }
}
