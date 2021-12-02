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
package org.sonarsource.text;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TextPhpTest extends TestBase {

  private static final String BASE_DIRECTORY = "projects/text-php/";
  private static final String NO_SONAR_PROFILE_NAME = "nosonar-profile";
  private static final String PROJECT_KEY = "textPhp";

  @Test
  public void test_php_nosonar() {
    ORCHESTRATOR.executeBuild(getSonarScanner(PROJECT_KEY, BASE_DIRECTORY, NO_SONAR_PROFILE_NAME));

    assertThat(getMeasureAsInt(PROJECT_KEY, "files")).isEqualTo(1);
    assertThat(getIssuesForRule(PROJECT_KEY, "common" + ":" + BIDI_RULE_ID)).hasSize(2);
  }
}
