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
package org.sonar.plugins.secrets.checks;

import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.plugins.common.Check;
import org.sonar.plugins.secrets.api.SpecificationBasedCheck;
import org.sonar.plugins.secrets.api.SpecificationLoader;
import org.sonar.plugins.secrets.utils.AbstractRuleExampleTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.plugins.common.TestUtils.analyze;
import static org.sonar.plugins.common.TestUtils.inputFile;

class GoogleCloudAccountKeyCheckTest extends AbstractRuleExampleTest {

  static Check check;

  GoogleCloudAccountKeyCheckTest() {
    super(new GoogleCloudAccountKeyCheck(), "S6335");
  }

  @BeforeAll
  public static void init() {
    check = new GoogleCloudAccountKeyCheck();
    SpecificationLoader specificationLoader = new SpecificationLoader();
    ((SpecificationBasedCheck) check).initialize(specificationLoader);
  }

  @Test
  void positive() throws Exception {
    InputFile file = inputFile(Path.of("src", "test", "resources", "checks", "GoogleCloudAccountKeyCheck", "GoogleCloudAccountPositive" +
      ".json"));
    assertThat(analyze(check, file)).containsExactly(
      "secrets:S6335 [5:18-5:1750] Make sure this GCP secret gets revoked, changed, and removed from the code.");
  }

  @Test
  void negative() throws Exception {
    InputFile file = inputFile(Path.of("src", "test", "resources", "checks", "GoogleCloudAccountKeyCheck", "GoogleCloudAccountNegative" +
      ".json"));
    assertThat(analyze(check, file)).isEmpty();
  }

}
