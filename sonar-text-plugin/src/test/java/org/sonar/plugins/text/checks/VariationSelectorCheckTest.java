/*
 * SonarQube Text Plugin
 * Copyright (C) 2021-2026 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.plugins.text.checks;

import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.plugins.common.TestUtils.analyze;
import static org.sonar.plugins.common.TestUtils.inputFileFromPath;
import static org.sonar.plugins.common.TestUtils.mockDurationStatistics;

class VariationSelectorCheckTest {

  @Test
  void shouldCheckFile() throws IOException {
    var check = new VariationSelectorCheck();
    check.initialize(mockDurationStatistics());
    var inputFile = inputFileFromPath(Path.of("src", "test", "resources", "checks", "VariationSelectorCheck", "test.js"));
    assertThat(analyze(check, inputFile)).containsExactly(
      "text:S8522 [5:0-5:32] This line contains 4 consecutive Variation Selector characters starting at column 21. Make sure that using consecutive variation selectors is intentional and safe here.",
      "text:S8522 [8:0-8:109] This line contains 8 consecutive Variation Selector characters starting at column 19. Make sure that using consecutive variation selectors is intentional and safe here.");
  }
}
