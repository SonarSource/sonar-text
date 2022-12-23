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
import org.sonar.api.SonarRuntime;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.plugins.text.checks.TextCheckList;
import org.sonar.plugins.text.text.TextLanguage;
import org.sonarsource.analyzer.commons.RuleMetadataLoader;


public class TextRuleDefinition implements RulesDefinition {

  public static final String REPOSITORY_KEY = "text";
  public static final String REPOSITORY_NAME = "SonarQube";

  static final String RESOURCE_FOLDER = "org/sonar/l10n/text/rules/text";
  static final String SONAR_WAY_PATH = "/" + RESOURCE_FOLDER +"/Sonar_way_profile.json";

  private final SonarRuntime sonarRuntime;

  public TextRuleDefinition(SonarRuntime sonarRuntime) {
    this.sonarRuntime = sonarRuntime;
  }

  @Override
  public void define(Context context) {
    NewRepository repository = context.createRepository(REPOSITORY_KEY, TextLanguage.KEY).setName(REPOSITORY_NAME);
    RuleMetadataLoader ruleMetadataLoader = new RuleMetadataLoader(RESOURCE_FOLDER, SONAR_WAY_PATH, sonarRuntime);
    ruleMetadataLoader.addRulesByAnnotatedClass(repository, new ArrayList<>(TextCheckList.checks()));
    repository.done();
  }
}
