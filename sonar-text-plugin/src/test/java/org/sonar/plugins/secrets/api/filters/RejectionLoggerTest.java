/*
 * SonarQube Text Plugin
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.plugins.secrets.api.filters;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mockito;
import org.slf4j.event.Level;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonar.plugins.common.InputFileContext;

import static org.assertj.core.api.Assertions.assertThat;

class RejectionLoggerTest {

  @RegisterExtension
  LogTesterJUnit5 logTester = new LogTesterJUnit5().setLevel(Level.DEBUG);

  @Test
  void disabledLoggerProducesNothing() {
    var logger = RejectionLogger.DISABLED;
    var context = new RejectionLogContext("S1", Mockito.mock(InputFileContext.class), 12);

    logger.log(context, "statistical (entropy): entropy=2.1");

    assertThat(logger.isEnabled()).isFalse();
    assertThat(logTester.logs(Level.DEBUG)).isEmpty();
  }

  @Test
  void enabledLoggerWithEmptyContextProducesNothing() {
    var logger = RejectionLogger.create(5);

    logger.log(RejectionLogContext.NONE, "patternNot: index=0");

    assertThat(logTester.logs(Level.DEBUG)).isEmpty();
  }

  @Test
  void enabledLoggerEmitsOneLinePerRejection() {
    var logger = RejectionLogger.create(5);
    var fileContext = mockFileContext("path/to/file.yaml");
    var context = new RejectionLogContext("S6290", fileContext, 42);

    logger.log(context, "statistical (entropy): entropy=2.1, threshold=3.5");

    assertThat(logTester.logs(Level.DEBUG)).hasSize(1)
      .first().asString()
      .contains("rule=S6290")
      .contains("file=path/to/file.yaml")
      .contains("offset=42")
      .contains("by statistical (entropy): entropy=2.1, threshold=3.5");
  }

  @Test
  void rateLimitSuppressesAfterThreshold() {
    var logger = RejectionLogger.create(2);
    var fileContext = mockFileContext("file.yaml");
    var context = new RejectionLogContext("S6290", fileContext, 0);

    for (int i = 0; i < 5; i++) {
      logger.log(context, "patternNot: index=" + i);
    }

    // 2 real entries + 1 "further rejections suppressed" summary; the remaining 2 hits emit nothing
    assertThat(logTester.logs(Level.DEBUG)).hasSize(3);
    assertThat(logTester.logs(Level.DEBUG).get(2))
      .contains("Further candidate rejections suppressed")
      .contains("rule=S6290")
      .contains("file=file.yaml")
      .contains("after 2 entries");
  }

  @Test
  void rateLimitsAreIndependentPerRuleAndFile() {
    var logger = RejectionLogger.create(1);
    var ctxRuleA = new RejectionLogContext("S1", mockFileContext("a.yaml"), 0);
    var ctxRuleB = new RejectionLogContext("S2", mockFileContext("a.yaml"), 0);
    var ctxOtherFile = new RejectionLogContext("S1", mockFileContext("b.yaml"), 0);

    logger.log(ctxRuleA, "heuristics: uri");
    logger.log(ctxRuleA, "heuristics: uri"); // suppressed summary
    logger.log(ctxRuleB, "heuristics: uri");
    logger.log(ctxOtherFile, "heuristics: uri");

    // First hit on each (rule, file) pair + a single summary for the second hit on (S1, a.yaml)
    assertThat(logTester.logs(Level.DEBUG)).hasSize(4);
  }

  private static InputFileContext mockFileContext(String displayName) {
    var fileContext = Mockito.mock(InputFileContext.class);
    Mockito.when(fileContext.toString()).thenReturn(displayName);
    return fileContext;
  }
}
