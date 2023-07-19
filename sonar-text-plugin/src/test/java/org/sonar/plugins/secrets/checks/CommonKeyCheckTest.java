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

import java.io.IOException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.sonar.plugins.common.Check;
import org.sonar.plugins.secrets.api.SpecificationBasedCheck;
import org.sonar.plugins.secrets.api.SpecificationLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.plugins.common.TestUtils.analyze;

//TODO SONARTEXT-51 Validate functionality of detection logic for specified secrets
@java.lang.SuppressWarnings("squid:S6652")
class CommonKeyCheckTest {

  static Check check;

  @BeforeAll
  public static void init() {
    check = new CommonKeyCheck();
    SpecificationLoader specificationLoader = new SpecificationLoader();
    ((SpecificationBasedCheck) check).initialize(specificationLoader);
  }

  @Test
  void shouldHaveRightAmountOfMatchers() {
    assertThat(check.ruleKey.rule()).isEqualTo("S6652");
    assertThat(((SpecificationBasedCheck) check).getMatcher()).hasSize(54);
  }

  @Test
  void shouldFindIssue() throws IOException {
    String fileContent = "let requestHeaders: HTTPHeaders = [\n" +
      "                \"Authorization\": \"Key d819f799b90bc8dbaffd83661782dbb7\",\n" +
      "                \"Content-Type\": \"application/json\"\n" +
      "            ]\n" +
      "            \n" +
      "            Alamofire.request(\n" +
      "                \"https://api.clarifai.com/v2/models/c76825c96cbac79521a200cb54b7f09b/outputs\"";
    assertThat(analyze(check, fileContent)).containsExactly(
      "secrets:S6652 [2:38-2:70] Make sure this Clarifai key gets revoked, changed, and removed from the code.");
  }

  @Test
  void shouldNotFindIssueBecausePatternAroundIsMissing() throws IOException {
    String fileContent = "let requestHeaders: HTTPHeaders = [\n" +
      "                \"Authorization\": \"Key d819f799b90bc8dbaffd83661782dbb7\",\n" +
      "                \"Content-Type\": \"application/json\"\n" +
      "            ]\n" +
      "            \n" +
      "            Alamofire.request(\n" +
      "                \"https://api.x.com/v2/models/c76825c96cbac79521a200cb54b7f09b/outputs\"";
    assertThat(analyze(check, fileContent)).isEmpty();
  }

  @Test
  void shouldFindIssueAsGithubSecretWithStatFilterInputString() throws IOException {
    String fileContent = "gh_token=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";
    assertThat(analyze(check, fileContent)).containsExactly(
      "secrets:S6652 [1:9-1:49] Make sure this Github token gets revoked, changed, and removed from the code.");
  }
}
