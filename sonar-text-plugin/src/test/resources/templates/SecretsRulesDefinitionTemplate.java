/*
 * SonarQube Text Plugin
 * Copyright (C) 2021-2023 SonarSource SA
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
import org.sonar.api.SonarRuntime;
import org.sonar.plugins.common.CommonRulesDefinition;
import org.sonar.plugins.common.DefaultQualityProfileDefinition;
//<REPLACE-WITH-IMPORTS-OF-ALL-CHECKS>
public class SecretsRulesDefinition extends CommonRulesDefinition {

  public static final String REPOSITORY_KEY = "secrets";
  public static final String REPOSITORY_NAME = "Sonar Secrets Analyzer";

  public SecretsRulesDefinition(SonarRuntime sonarRuntime) {
    super(sonarRuntime, REPOSITORY_KEY, REPOSITORY_NAME, SecretsLanguage.KEY, checks());
  }

  public static class DefaultQualityProfile extends DefaultQualityProfileDefinition {
    public DefaultQualityProfile() {
      super(REPOSITORY_KEY, SecretsLanguage.KEY);
    }
  }

  //<REPLACE-WITH-LIST-OF-CHECKS>
}