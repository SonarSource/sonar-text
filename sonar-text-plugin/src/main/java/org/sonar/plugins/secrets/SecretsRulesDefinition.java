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
package org.sonar.plugins.secrets;

import java.util.List;
import org.sonar.api.SonarRuntime;
import org.sonar.plugins.common.CommonRulesDefinition;
import org.sonar.plugins.common.DefaultQualityProfileDefinition;
import org.sonarsource.analyzer.commons.BuiltInQualityProfileJsonLoader;

public class SecretsRulesDefinition extends CommonRulesDefinition {

  public static final String REPOSITORY_KEY = "secrets";
  public static final String REPOSITORY_NAME = "Sonar Secrets Analyzer";

  public SecretsRulesDefinition(SonarRuntime sonarRuntime) {
    super(sonarRuntime, REPOSITORY_KEY, REPOSITORY_NAME, SecretsLanguage.KEY);
  }

  public static class DefaultQualityProfile extends DefaultQualityProfileDefinition {
    public DefaultQualityProfile(SonarRuntime sonarRuntime) {
      super(sonarRuntime, REPOSITORY_KEY, SecretsLanguage.KEY);
    }

    @Override
    public void define(Context context) {
      NewBuiltInQualityProfile profile = context.createBuiltInQualityProfile(NAME, languageKey);
      // load the default profile from the open-source plugin ("org" package)
      BuiltInQualityProfileJsonLoader.load(profile, repositoryKey, profilePath(DEFAULT_PACKAGE_PREFIX, repositoryKey, languageKey));
      if (isCommercialEdition()) {
        // load the default profile from the commercial plugin ("com" package)
        BuiltInQualityProfileJsonLoader.load(profile, repositoryKey, profilePath(packagePrefix(), repositoryKey, languageKey));
      }
      // load the rules that are part of "Sonar way" on SonarQube Cloud only
      loadSonarQubeCloudOnlyRules(profile);
      profile.setDefault(true);
      profile.done();
    }

    @Override
    protected List<String> sonarQubeCloudOnlyProfilePaths() {
      // only the commercial plugins ship a SonarQube Cloud addition profile for the secrets repository
      if (!isCommercialEdition()) {
        return List.of();
      }
      return List.of(sonarQubeCloudProfilePath(packagePrefix(), repositoryKey, languageKey));
    }

    private boolean isCommercialEdition() {
      return !DEFAULT_PACKAGE_PREFIX.equals(packagePrefix());
    }
  }

  @Override
  public List<Class<?>> checks() {
    // The list now is generated dynamically, the generation logic can be found in
    // org.sonarsource.text.check-list-generator.gradle.kts
    return new SecretsCheckList().checks();
  }
}
