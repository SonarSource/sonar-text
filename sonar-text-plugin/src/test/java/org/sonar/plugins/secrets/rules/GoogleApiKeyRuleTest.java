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
package org.sonar.plugins.secrets.rules;

import java.util.List;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.sonar.plugins.secrets.TestUtils.aNormalizedInputFile;

public class GoogleApiKeyRuleTest {
  private final GoogleApiKeyRule underTest = new GoogleApiKeyRule();

  @Test
  public void testRuleProperties() {
    assertThat(underTest.getRuleKey()).isEqualTo("S6334");
    assertThat(underTest.getMessage()).isEqualTo("Make sure this Google API Key is not disclosed.");
  }

  @Test
  public void testRuleRegexPositive() {
    List<Secret> secrets = underTest.findSecretsIn(aNormalizedInputFile("android:value=\"AIzaSyCis4NzxMw1aJyvUIrjGILjPkSdxrRfof4\""));

    assertThat(secrets)
      .extracting(Secret::getTextRange)
      .extracting("start.line", "start.lineOffset", "end.line", "end.lineOffset")
      .containsOnly(tuple(1, 15, 1, 54));
  }

  @Test
  public void testRuleRegexNegative() {
    List<Secret> secrets = underTest.findSecretsIn(aNormalizedInputFile("android:value=\"AIzaSyCis4NzxMw1aJyvUIrjGfof4\""));

    assertThat(secrets).isEmpty();
  }
}
