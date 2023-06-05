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

class IbmApiKeyCheckTest {
  static Check check;

  @BeforeAll
  public static void init() {
    check = new IbmApiKeyCheck();
    SpecificationLoader specificationLoader = new SpecificationLoader();
    ((SpecificationBasedCheck) check).initialize(specificationLoader);
  }

  @Test
  void positive() throws IOException {
    String fileContent = "\"apikey\": \"iT5wxMGq2-ZJlMAHYoODl5EuTeCPvNRkSp1h3m99HWrc\"";
    assertThat(analyze(check, fileContent)).containsExactly(
      "secrets:S6337 [1:11-1:55] Make sure this IBM API key gets revoked, changed, and removed from the code.");
  }

  @Test
  void negative() throws IOException {
    String fileContent = "\"apikey\": \"iT5wxMGq2-ZJlMAHYoODl5EuTeCPvWrc\"";
    assertThat(analyze(check, fileContent)).isEmpty();
  }

  @Test
  void negative_low_entropy() throws IOException {
    String fileContent = "\"apikey\": \"01234567890123456789012345678901234567890123\"";
    assertThat(analyze(check, fileContent)).isEmpty();
  }

}
