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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.sonar.plugins.common.Check;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.plugins.common.TestUtils.analyze;

class MwsAuthTokenCheckTest {

  Check check = new MwsAuthTokenCheck();

  @Test
  void positive() throws IOException {
    String fileContent = "export MWS_TOKEN=amzn.mws.4ea38b7b-f563-7709-4bae-12ba540c0ac5";
    assertThat(analyze(check, fileContent)).containsExactly(
      "secrets:S6292 [1:17-1:62] Make sure this Amazon MWS Auth Token is not disclosed.");
  }

  @ParameterizedTest
  @ValueSource(strings = {
          "export MWS_TOKEN=amz.mws.4ea38b7b-f563-7709-4bae-12ba540c0ac5",
          // extra characters before
          "export MWS_TOKEN=blamzn.mws.4ea38b7b-f563-7709-4bae-12ba540c0ac5",
          // extra characters after
          "export MWS_TOKEN=amzn.mws.4ea38b7b-f563-7709-4bae-12ba540c0ac52222222222222"
  })
  void negative() throws IOException {
    String fileContent = "export MWS_TOKEN=amz.mws.4ea38b7b-f563-7709-4bae-12ba540c0ac5";
    assertThat(analyze(check, fileContent)).isEmpty();
  }

}
