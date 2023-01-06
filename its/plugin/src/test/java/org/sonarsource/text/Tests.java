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
package org.sonarsource.text;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.locator.MavenLocation;
import java.io.File;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
  NoSonarTest.class,
  TextPhpTest.class
})
public class Tests {

  static final String SQ_VERSION_PROPERTY = "sonar.runtimeVersion";
  static final String DEFAULT_SQ_VERSION = "LATEST_RELEASE";
  static final String SONAR_PHP_VERSION = "LATEST_RELEASE";

  @ClassRule
  public static final Orchestrator ORCHESTRATOR;

  public static final FileLocation PLUGIN_LOCATION = FileLocation.byWildcardMavenFilename(new File("../../sonar-text-plugin/target"), "sonar-text-plugin-*.jar");

  static {
    ORCHESTRATOR = Orchestrator.builderEnv()
      .useDefaultAdminCredentialsForBuilds(true)
      .setSonarVersion(System.getProperty(SQ_VERSION_PROPERTY, DEFAULT_SQ_VERSION))
      .addPlugin(PLUGIN_LOCATION)
      // sonar-php-plugin is used to bring the NOSONAR filter to php files.
      // It means someone could use this trick to hide a S6389 issue, it's probably something to fix.
      .addPlugin(MavenLocation.of("org.sonarsource.php", "sonar-php-plugin", SONAR_PHP_VERSION))
      .restoreProfileAtStartup(FileLocation.of("src/test/resources/nosonar-text.xml"))
      .build();
  }
}
