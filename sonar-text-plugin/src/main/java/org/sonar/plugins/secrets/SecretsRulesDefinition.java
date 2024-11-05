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
package org.sonar.plugins.secrets;

import java.util.List;
import org.sonar.api.SonarEdition;
import org.sonar.api.SonarRuntime;
import org.sonar.plugins.common.CommonRulesDefinition;
import org.sonar.plugins.common.DefaultQualityProfileDefinition;

public class SecretsRulesDefinition extends CommonRulesDefinition {

  public static final String REPOSITORY_KEY = "secrets";
  public static final String REPOSITORY_NAME = "Sonar Secrets Analyzer";

  public SecretsRulesDefinition(SonarRuntime sonarRuntime) {
    super(sonarRuntime, REPOSITORY_KEY, REPOSITORY_NAME, SecretsLanguage.KEY);
  }

  public static class DefaultQualityProfile extends DefaultQualityProfileDefinition {
    private final SonarRuntime sonarRuntime;

    public DefaultQualityProfile(SonarRuntime sonarRuntime) {
      super(REPOSITORY_KEY, SecretsLanguage.KEY);
      this.sonarRuntime = sonarRuntime;
    }

    @Override
    public void define(Context context) {
      if (sonarRuntime.getEdition() != SonarEdition.COMMUNITY) {
        return;
      }
      super.define(context);
    }
  }

  @Override
  public List<Class<?>> checks() {
    // The list now is generated dynamically, the generation logic can be found in
    // org.sonarsource.text.check-list-generator.gradle.kts
    return new SecretsCheckList().checks();
  }
}
