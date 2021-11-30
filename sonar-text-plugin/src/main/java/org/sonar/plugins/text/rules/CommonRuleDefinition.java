/*
 * SonarQube Text Plugin
 * Copyright (C) 2021-2021 SonarSource SA
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
package org.sonar.plugins.text.rules;

import java.util.ArrayList;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.plugins.text.CommonPlugin;
import org.sonar.plugins.text.core.CommonLanguage;
import org.sonar.plugins.text.checks.CheckList;
import org.sonarsource.analyzer.commons.RuleMetadataLoader;

import static org.sonar.plugins.text.CommonPlugin.REPOSITORY_NAME;

public class CommonRuleDefinition implements RulesDefinition {

  static final String RESOURCE_FOLDER = "org/sonar/l10n/common/rules/common";

  @Override
  public void define(Context context) {
    NewRepository repository = context.createRepository(CommonPlugin.REPOSITORY_KEY, CommonLanguage.KEY).setName(REPOSITORY_NAME);
    RuleMetadataLoader ruleMetadataLoader = new RuleMetadataLoader(RESOURCE_FOLDER);
    ruleMetadataLoader.addRulesByAnnotatedClass(repository, new ArrayList<>(CheckList.checks()));
    repository.done();
  }
}
