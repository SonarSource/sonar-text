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
package org.sonar.plugins.secrets.checks;

import java.io.IOException;
import java.util.Collection;
import org.junit.Test;
import org.sonar.api.batch.sensor.issue.Issue;
import org.sonar.plugins.common.Check;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.plugins.common.TestUtils.analyze;
import static org.sonar.plugins.common.TestUtils.asString;
import static org.sonar.plugins.common.TestUtils.inputFile;

public class GoogleApiKeyCheckTest {
  Check check = new GoogleApiKeyCheck();

  @Test
  public void testRuleRegexPositive() throws IOException {
    Collection<Issue> issues = analyze(check, inputFile("android:value=\"AIzaSyCis4NzxMw1aJyvUIrjGILjPkSdxrRfof4\""));
    assertThat(asString(issues)).containsExactly(
      "secrets:S6334 [1:15-1:54] Make sure this Google API Key is not disclosed.");
  }

  @Test
  public void testRuleRegexNegative() throws IOException {
    Collection<Issue> issues = analyze(check, inputFile("android:value=\"AIzaSyCis4NzxMw1aJyvUIrjGfof4\""));
    assertThat(issues).isEmpty();
  }

}
