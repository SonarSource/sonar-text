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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.sonar.plugins.common.Check;
import org.sonar.plugins.secrets.api.SpecificationBasedCheck;
import org.sonar.plugins.secrets.api.SpecificationLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.plugins.common.TestUtils.analyze;

@java.lang.SuppressWarnings("squid:S6334")
class GoogleApiKeyCheckTest {

  static Check check;

  @BeforeAll
  public static void init() {
    check = new GoogleApiKeyCheck();
    SpecificationLoader specificationLoader = new SpecificationLoader();
    ((SpecificationBasedCheck) check).initialize(specificationLoader);
  }

  @Test
  void positive() throws IOException {
    String fileContent = "android:value=\"AIzaSyCis4NzxMw1aJyvUIrjGILjPkSdxrRfof4\"";
    assertThat(analyze(check, fileContent)).containsExactly(
      "secrets:S6334 [1:15-1:54] Make sure this Google API Key gets revoked, changed, and removed from the code.");
  }

  @ParameterizedTest
  @ValueSource(strings = {
    "android:value=\"AIzaSyCis4NzxMw1aJyvUIrjGfof4\"",
    // extra characters before
    "android:value=\"KatitioAIzaSyCis4NzxMw1aJyvUIrjGILjPkSdxrRfof4\"",
    // extra characters after
    "android:value=\"AIzaSyCis4NzxMw1aJyvUIrjGfof4abc\""
  })
  void negative(String fileContent) throws IOException {
    assertThat(analyze(check, fileContent)).isEmpty();
  }

}
