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
package org.sonar.plugins.text.rules;

import java.util.ArrayList;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.plugins.text.TextPlugin;
import org.sonar.plugins.text.checks.CheckList;
import org.sonar.plugins.text.core.TextLanguage;
import org.sonarsource.analyzer.commons.RuleMetadataLoader;

import static org.sonar.plugins.text.TextPlugin.REPOSITORY_NAME;

public class TextRuleDefinition implements RulesDefinition {

  static final String RESOURCE_FOLDER = "org/sonar/l10n/text/rules/text";

  @Override
  public void define(Context context) {
    NewRepository repository = context.createRepository(TextPlugin.REPOSITORY_KEY, TextLanguage.KEY).setName(REPOSITORY_NAME);
    RuleMetadataLoader ruleMetadataLoader = new RuleMetadataLoader(RESOURCE_FOLDER);
    ruleMetadataLoader.addRulesByAnnotatedClass(repository, new ArrayList<>(CheckList.checks()));
    repository.done();
  }
}
