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

import java.util.List;
import java.util.stream.Collectors;
import org.sonar.api.SonarRuntime;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.plugins.secrets.SecretLanguage;
import org.sonarsource.analyzer.commons.RuleMetadataLoader;

public class SecretsRulesDefinition implements RulesDefinition {

  public static final String REPOSITORY_KEY = "secrets";
  public static final String REPOSITORY_NAME = "Sonar Secrets Analyzer";
  private static final String RESOURCE_BASE_PATH = "/org/sonar/l10n/secrets/rules/secrets";

  public static final String SONAR_WAY_PATH = "/org/sonar/l10n/secrets/rules/secrets/Sonar_way_profile.json";

  private final SonarRuntime sonarRuntime;

  public SecretsRulesDefinition(SonarRuntime sonarRuntime) {
    this.sonarRuntime = sonarRuntime;
  }

  @Override
  public void define(Context context) {
    NewRepository repository = context.createRepository(REPOSITORY_KEY, SecretLanguage.KEY).setName(REPOSITORY_NAME);

    RuleMetadataLoader ruleMetadataLoader = new RuleMetadataLoader(RESOURCE_BASE_PATH, SONAR_WAY_PATH, sonarRuntime);
    List<String> ruleKeys = SecretCheckList.createInstances().stream()
            .map(SecretRule::getRuleKey)
            .distinct().collect(Collectors.toList());
    ruleMetadataLoader.addRulesByRuleKey(repository, ruleKeys);

    repository.done();
  }

}
