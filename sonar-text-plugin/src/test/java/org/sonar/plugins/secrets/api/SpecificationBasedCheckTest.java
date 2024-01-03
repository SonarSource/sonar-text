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
package org.sonar.plugins.secrets.api;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.sonar.check.Rule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.plugins.common.TestUtils.analyze;
import static org.sonar.plugins.secrets.utils.TestUtils.mockDurationStatistics;

class SpecificationBasedCheckTest {

  @Test
  void checkShouldRaiseIssueOnBasicDetection() throws IOException {
    String specificationLocation = "secretsConfiguration/";
    Set<String> specifications = Set.of("validMinSpec.yaml");
    SpecificationLoader specificationLoader = new SpecificationLoader(specificationLocation, specifications);

    String fileContent = "The content contains the rule matching pattern and various other characters.";
    ExampleCheck exampleCheck = new ExampleCheck();
    exampleCheck.initialize(specificationLoader, new HashMap<>(), mockDurationStatistics());

    assertThat(analyze(exampleCheck, fileContent)).containsExactly(
      "secrets:exampleKey [1:25-1:46] provider message");
  }

  @Test
  void checkShouldNotRaiseIssueWithPostFilterBecauseOfLowEntropy() throws IOException {
    String specificationLocation = "secretsConfiguration/";
    Set<String> specifications = Set.of("validReferenceSpec.yaml");
    SpecificationLoader specificationLoader = new SpecificationLoader(specificationLocation, specifications);

    String fileContent = "rule matching pattern with low entropy";
    ExampleCheck exampleCheck = new ExampleCheck();
    exampleCheck.initialize(specificationLoader, new HashMap<>(), mockDurationStatistics());

    assertThat(analyze(exampleCheck, fileContent)).isEmpty();
  }

  @Test
  void checkShouldRaiseIssueWhenFilterHasLowEntropyThreshold() throws IOException {
    String specificationLocation = "secretsConfiguration/postFilter/";
    Set<String> specifications = Set.of("postFilterSpec.yaml");
    SpecificationLoader specificationLoader = new SpecificationLoader(specificationLocation, specifications);
    specificationLoader.getRulesForKey("exampleKey").get(0).getDetection().getPost().getStatisticalFilter().setThreshold(3f);

    String fileContent = "rule matching pattern and post filter has low entropy";
    ExampleCheck exampleCheck = new ExampleCheck();
    exampleCheck.initialize(specificationLoader, new HashMap<>(), mockDurationStatistics());

    assertThat(analyze(exampleCheck, fileContent)).containsExactly(
      "secrets:exampleKey [1:0-1:21] rule message");
  }

  @Test
  void checkShouldNotRaiseIssueWithPostFilterBecauseOfPatternNot() throws IOException {
    String specificationLocation = "secretsConfiguration/postFilter/";
    Set<String> specifications = Set.of("postFilterSpec.yaml");
    SpecificationLoader specificationLoader = new SpecificationLoader(specificationLocation, specifications);
    specificationLoader.getRulesForKey("exampleKey").get(0).getDetection().getPost().setPatternNot(List.of("matching"));
    specificationLoader.getRulesForKey("exampleKey").get(0).getDetection().getPost().getStatisticalFilter().setThreshold(3f);

    String fileContent = "rule matching pattern and patternNot matching inside and post filter has low entropy";
    ExampleCheck exampleCheck = new ExampleCheck();
    exampleCheck.initialize(specificationLoader, new HashMap<>(), mockDurationStatistics());

    assertThat(analyze(exampleCheck, fileContent)).isEmpty();
  }

  @Rule(key = "exampleKey")
  static class ExampleCheck extends SpecificationBasedCheck {

  }

}
