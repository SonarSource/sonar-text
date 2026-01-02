/*
 * SonarQube Text Plugin
 * Copyright (C) 2021-2026 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.plugins.common;

import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition;
import org.sonarsource.analyzer.commons.BuiltInQualityProfileJsonLoader;

public class DefaultQualityProfileDefinition implements BuiltInQualityProfilesDefinition {

  public static final String NAME = "Sonar way";
  public static final String FILE_NAME = "Sonar_way_profile.json";

  public final String repositoryKey;
  public final String languageKey;

  public DefaultQualityProfileDefinition(String repositoryKey, String languageKey) {
    this.repositoryKey = repositoryKey;
    this.languageKey = languageKey;
  }

  public void define(BuiltInQualityProfilesDefinition.Context context) {
    BuiltInQualityProfilesDefinition.NewBuiltInQualityProfile profile = context.createBuiltInQualityProfile(NAME, languageKey);
    BuiltInQualityProfileJsonLoader.load(profile, repositoryKey, profilePath(packagePrefix(), repositoryKey, languageKey));
    profile.setDefault(true);
    profile.done();
  }

  public static String profilePath(String packagePrefix, String repository, String language) {
    return CommonRulesDefinition.resourcePath(packagePrefix, repository, language) + "/" + FILE_NAME;
  }

  public String packagePrefix() {
    return "org";
  }
}
