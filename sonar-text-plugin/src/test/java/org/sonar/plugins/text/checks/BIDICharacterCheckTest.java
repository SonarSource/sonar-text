/*
 * SonarQube Text Plugin
 * Copyright (C) 2021-2021 SonarSource SA
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
package org.sonar.plugins.text.checks;

import org.junit.jupiter.api.Test;
import org.sonar.plugins.text.CheckVerifier;

class BIDICharacterCheckTest {

  @Test
  void test() {
    CheckVerifier.verify(new BIDICharacterCheck(), "BIDICharacterCheck/test.php",
      new CheckVerifier.LineIssue(3, "" +
        "This line contains a bidirectional character in column 12. Make sure that using bidirectional characters is safe here."),
      new CheckVerifier.LineIssue(4),
      new CheckVerifier.LineIssue(5),
      new CheckVerifier.LineIssue(6),
      new CheckVerifier.LineIssue(7),
      new CheckVerifier.LineIssue(8),
      new CheckVerifier.LineIssue(9),
      new CheckVerifier.LineIssue(12),
      new CheckVerifier.LineIssue(13),
      new CheckVerifier.LineIssue(16,
        "This line contains a bidirectional character in column 32. Make sure that using bidirectional characters is safe here."),
      new CheckVerifier.LineIssue(17,
        "This line contains a bidirectional character in column 32. Make sure that using bidirectional characters is safe here."),
      new CheckVerifier.LineIssue(20,
        "This line contains a bidirectional character in column 12. Make sure that using bidirectional characters is safe here."),
      new CheckVerifier.LineIssue(21));
  }
}