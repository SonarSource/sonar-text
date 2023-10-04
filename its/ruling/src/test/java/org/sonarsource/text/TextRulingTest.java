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
package org.sonarsource.text;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarScanner;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.locator.MavenLocation;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonarsource.analyzer.commons.ProfileGenerator;

import static org.assertj.core.api.Assertions.assertThat;

public class TextRulingTest {

  private static final String SQ_VERSION_PROPERTY = "sonar.runtimeVersion";
  private static final String DEFAULT_SQ_VERSION = "LATEST_RELEASE";
  private static final String LITS_PLUGIN_VERSION = "0.11.0.2659";
  private static final File LITS_OUTPUT_DIRECTORY = FileLocation.of("build/reports/lits").getFile();
  private static final File LITS_DIFFERENCES_FILE = Path.of(LITS_OUTPUT_DIRECTORY.toURI()).resolve("differences").toFile();

  @ClassRule
  public static Orchestrator ORCHESTRATOR = Orchestrator.builderEnv()
    .useDefaultAdminCredentialsForBuilds(true)
    .setSonarVersion(System.getProperty(SQ_VERSION_PROPERTY, DEFAULT_SQ_VERSION))
    .addPlugin(FileLocation.byWildcardMavenFilename(new File("../../sonar-text-plugin/build/libs"), "sonar-text-plugin-*-all.jar"))
    .addPlugin(MavenLocation.of("org.sonarsource.sonar-lits-plugin", "sonar-lits-plugin", LITS_PLUGIN_VERSION))
    .build();

  @BeforeClass
  public static void prepare_quality_profile() throws IOException {
    ProfileGenerator.RulesConfiguration parameters = new ProfileGenerator.RulesConfiguration();

    String serverUrl = ORCHESTRATOR.getServer().getUrl();
    File textProfileFile = ProfileGenerator.generateProfile(serverUrl, "text", "text", parameters, Collections.emptySet());
    ORCHESTRATOR.getServer().restoreProfile(FileLocation.of(textProfileFile));
    File secretsProfileFile = ProfileGenerator.generateProfile(serverUrl, "secrets", "secrets", parameters, Collections.emptySet());
    ORCHESTRATOR.getServer().restoreProfile(FileLocation.of(secretsProfileFile));

    Files.createDirectories(Path.of(LITS_DIFFERENCES_FILE.getParentFile().toURI()));
  }

  @Test
  public void test() throws Exception {
    ORCHESTRATOR.getServer().provisionProject("project", "project");
    ORCHESTRATOR.getServer().associateProjectToQualityProfile("project", "text", "rules");
    ORCHESTRATOR.getServer().associateProjectToQualityProfile("project", "secrets", "rules");
    SonarScanner build = SonarScanner.create(FileLocation.of("src/test/resources/sources").getFile())
      .setProjectKey("project")
      .setProjectName("project")
      .setProjectVersion("1")
      .setSourceDirs(".")
      .setSourceEncoding("UTF-8")
      .setDebugLogs(true)
      .setProperty("sonar.lits.dump.old", FileLocation.of("src/test/resources/expected").getFile().getAbsolutePath())
      .setProperty("sonar.lits.dump.new", FileLocation.of(LITS_OUTPUT_DIRECTORY + "/actual").getFile().getAbsolutePath())
      .setProperty("sonar.text.analyzeAllFiles", "true")
      .setProperty("sonar.cpd.exclusions", "**/*")
      .setProperty("sonar.lits.differences", LITS_DIFFERENCES_FILE.getAbsolutePath())
      .setProperty("sonar.text.duration.statistics", "true");
    build.setEnvironmentVariable("SONAR_RUNNER_OPTS", "-Xmx1000m");
    ORCHESTRATOR.executeBuild(build);

    String litsDifference = new String(Files.readAllBytes(LITS_DIFFERENCES_FILE.toPath()));
    assertThat(litsDifference).isEmpty();
  }
}
