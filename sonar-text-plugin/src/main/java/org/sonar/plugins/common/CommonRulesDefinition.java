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
package org.sonar.plugins.common;

import java.util.List;
import org.sonar.api.SonarRuntime;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonarsource.analyzer.commons.RuleMetadataLoader;

public class CommonRulesDefinition implements RulesDefinition {

  public static final String RESOURCE_FOLDER_FORMAT = "/org/sonar/l10n/%s/rules/%s";

  private final SonarRuntime sonarRuntime;
  public final String repositoryKey;
  public final String repositoryName;
  public final String languageKey;
  private final List<Class<?>> checks;

  public CommonRulesDefinition(SonarRuntime sonarRuntime, String repositoryKey, String repositoryName,
    String languageKey, List<Class<?>> checks) {
    this.sonarRuntime = sonarRuntime;
    this.repositoryKey = repositoryKey;
    this.repositoryName = repositoryName;
    this.languageKey = languageKey;
    this.checks = checks;
  }

  @Override
  public void define(Context context) {
    NewRepository repository = context.createRepository(repositoryKey, languageKey).setName(repositoryName);
    String resourcePath = resourcePath(repositoryKey, languageKey);
    String defaultProfilePath = DefaultQualityProfileDefinition.profilePath(repositoryKey, languageKey);
    RuleMetadataLoader ruleMetadataLoader = new RuleMetadataLoader(resourcePath, defaultProfilePath, sonarRuntime);
    ruleMetadataLoader.addRulesByAnnotatedClass(repository, checks);
    repository.done();
  }

  public static String resourcePath(String repository, String language) {
    return String.format(RESOURCE_FOLDER_FORMAT, repository, language);
  }

}
