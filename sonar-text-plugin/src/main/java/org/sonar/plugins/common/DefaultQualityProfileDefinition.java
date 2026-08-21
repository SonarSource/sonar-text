/*
 * SonarQube Text Plugin
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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

import java.util.List;
import org.sonar.api.SonarRuntime;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition;
import org.sonarsource.analyzer.commons.BuiltInQualityProfileJsonLoader;

public class DefaultQualityProfileDefinition implements BuiltInQualityProfilesDefinition {

  public static final String NAME = "Sonar way";
  public static final String FILE_NAME = "Sonar_way_profile.json";
  /**
   * Name of the profile file listing the rules that are part of "Sonar way" on SonarQube Cloud only. Such rules are
   * rolled out gradually on SonarQube Cloud, per organization, through the feature flags declared in
   * {@link TextAndSecretsSensor}, before being enabled on every product. Only the repositories that have such rules
   * ship this file.
   */
  public static final String SONARQUBE_CLOUD_FILE_NAME = "Sonar_way_sonarqube_cloud_addition_profile.json";

  public final String repositoryKey;
  public final String languageKey;
  protected final SonarRuntime sonarRuntime;

  public DefaultQualityProfileDefinition(SonarRuntime sonarRuntime, String repositoryKey, String languageKey) {
    this.sonarRuntime = sonarRuntime;
    this.repositoryKey = repositoryKey;
    this.languageKey = languageKey;
  }

  public void define(BuiltInQualityProfilesDefinition.Context context) {
    BuiltInQualityProfilesDefinition.NewBuiltInQualityProfile profile = context.createBuiltInQualityProfile(NAME, languageKey);
    BuiltInQualityProfileJsonLoader.load(profile, repositoryKey, profilePath(packagePrefix(), repositoryKey, languageKey));
    loadSonarQubeCloudOnlyRules(profile);
    profile.setDefault(true);
    profile.done();
  }

  /**
   * Activates the rules returned by {@link #sonarQubeCloudOnlyProfilePaths()}, but only when the analyzer runs on
   * SonarQube Cloud. On SonarQube Server and SonarQube IDE the profile is left untouched.
   */
  protected final void loadSonarQubeCloudOnlyRules(NewBuiltInQualityProfile profile) {
    if (!TextAndSecretsSensor.isSonarCloudContext(sonarRuntime)) {
      return;
    }
    for (String path : sonarQubeCloudOnlyProfilePaths()) {
      BuiltInQualityProfileJsonLoader.load(profile, repositoryKey, path);
    }
  }

  /**
   * Resource paths of the profile files holding the rules that belong to "Sonar way" on SonarQube Cloud only. Empty
   * by default: for most repositories "Sonar way" is the same on every product. The rule keys they contain must not
   * already be listed in the regular profile files, as a rule cannot be activated twice.
   */
  protected List<String> sonarQubeCloudOnlyProfilePaths() {
    return List.of();
  }

  public static String profilePath(String packagePrefix, String repository, String language) {
    return CommonRulesDefinition.resourcePath(packagePrefix, repository, language) + "/" + FILE_NAME;
  }

  public static String sonarQubeCloudProfilePath(String packagePrefix, String repository, String language) {
    return CommonRulesDefinition.resourcePath(packagePrefix, repository, language) + "/" + SONARQUBE_CLOUD_FILE_NAME;
  }

  public String packagePrefix() {
    return "org";
  }
}
