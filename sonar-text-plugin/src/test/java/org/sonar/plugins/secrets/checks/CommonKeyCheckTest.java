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

import org.junit.jupiter.api.Test;
import org.sonar.plugins.common.Check;
import org.sonar.plugins.secrets.api.SpecificationBasedCheck;
import org.sonar.plugins.secrets.utils.AbstractRuleExampleTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.plugins.common.TestUtils.analyze;

@SuppressWarnings("squid:S6652")
class CommonKeyCheckTest extends AbstractRuleExampleTest {
  public CommonKeyCheckTest() {
    super(new CommonKeyCheck());
  }

  @Test
  void shouldHaveRightAmountOfMatchers() {
    Check check = getInitializedCheck();
    assertThat(check.ruleKey.rule()).isEqualTo("S6652");
    assertThat(((SpecificationBasedCheck) check).getMatcher()).hasSize(54);
  }

  @Test
  void shouldNotFindIssueBecausePatternAroundIsMissing() throws IOException {
    Check check = getInitializedCheck();
    String fileContent = "let requestHeaders: HTTPHeaders = [\n" +
      "                \"Authorization\": \"Key d819f799b90bc8dbaffd83661782dbb7\",\n" +
      "                \"Content-Type\": \"application/json\"\n" +
      "            ]\n" +
      "            \n" +
      "            Alamofire.request(\n" +
      "                \"https://api.x.com/v2/models/c76825c96cbac79521a200cb54b7f09b/outputs\"";
    assertThat(analyze(check, fileContent)).isEmpty();
  }
}
