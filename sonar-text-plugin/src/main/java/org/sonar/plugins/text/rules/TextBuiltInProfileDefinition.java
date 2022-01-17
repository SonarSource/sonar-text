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

import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition;
import org.sonar.plugins.text.TextPlugin;
import org.sonar.plugins.text.core.TextLanguage;
import org.sonarsource.analyzer.commons.BuiltInQualityProfileJsonLoader;

public class TextBuiltInProfileDefinition implements BuiltInQualityProfilesDefinition {

  public static final String SONAR_WAY_PROFILE = "Sonar way";
  public static final String SONAR_WAY_PATH = "org/sonar/l10n/text/rules/text/Sonar_way_profile.json";

  @Override
  public void define(Context context) {
    NewBuiltInQualityProfile profile = context.createBuiltInQualityProfile(SONAR_WAY_PROFILE, TextLanguage.KEY);
    BuiltInQualityProfileJsonLoader.load(profile, TextPlugin.REPOSITORY_KEY, SONAR_WAY_PATH);
    profile.setDefault(true);
    profile.done();
  }

}
