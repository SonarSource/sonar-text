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
package org.sonar.plugins.secrets.api;

import java.io.IOException;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.sonar.check.Rule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.plugins.common.TestUtils.analyze;

class SpecificationBasedCheckTest {


  @Test
  void testShouldAnalyzeTest() throws IOException {
    String specificationLocation = "secretsConfiguration/";
    Set<String> specifications = Set.of("validMinSpec.yaml");
    SpecificationLoader.reinitialize(specificationLocation, specifications);

    String fileContent = "The content contains the test pattern and various other characters.";
    ExampleCheck exampleCheck = new ExampleCheck();
    assertThat(analyze(exampleCheck, fileContent)).containsExactly(
      "secrets:exampleKey [1:25-1:37] provider message"
    );
  }

  @Rule(key = "exampleKey")
  static class ExampleCheck extends SpecificationBasedCheck {

  }

}
