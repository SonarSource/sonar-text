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
package org.sonar.plugins.common;

import java.util.List;
import org.sonar.api.SonarRuntime;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonarsource.analyzer.commons.RuleMetadataLoader;

public abstract class CommonRulesDefinition implements RulesDefinition {

  private static final String RESOURCE_FOLDER_FORMAT = "/%s/sonar/l10n/%s/rules/%s";
  protected static final String DEFAULT_PACKAGE_PREFIX = "org";

  private final SonarRuntime sonarRuntime;
  public final String repositoryKey;
  public final String repositoryName;
  public final String languageKey;

  protected CommonRulesDefinition(SonarRuntime sonarRuntime, String repositoryKey, String repositoryName,
    String languageKey) {
    this.sonarRuntime = sonarRuntime;
    this.repositoryKey = repositoryKey;
    this.repositoryName = repositoryName;
    this.languageKey = languageKey;
  }

  @Override
  public void define(Context context) {
    var repository = context.createRepository(repositoryKey, languageKey).setName(repositoryName);

    loadRepository(packagePrefix(), repository, checks());
    repository.done();
  }

  public abstract List<Class<?>> checks();

  protected void loadRepository(String packagePrefix, RulesDefinition.NewRepository repository, List<Class<?>> checkClasses) {
    var resourcePath = resourcePath(packagePrefix, repositoryKey, languageKey);
    var defaultProfilePath = DefaultQualityProfileDefinition.profilePath(packagePrefix, repositoryKey, languageKey);
    var ruleMetadataLoader = new RuleMetadataLoader(resourcePath, defaultProfilePath, sonarRuntime);
    ruleMetadataLoader.addRulesByAnnotatedClass(repository, checkClasses);
  }

  public static String resourcePath(String packagePrefix, String repository, String language) {
    return String.format(RESOURCE_FOLDER_FORMAT, packagePrefix, repository, language);
  }

  public String packagePrefix() {
    return DEFAULT_PACKAGE_PREFIX;
  }
}
