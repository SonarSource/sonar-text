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

import javax.annotation.Nullable;
import org.sonar.plugins.common.InputFileContext;

/**
 * Context propagated through a post-filter chain so handlers can attach rule/file metadata
 * to a rejection log without ever leaking the candidate text or rule patterns.
 *
 * <p>Use {@link #NONE} from call sites that do not have rule/file context (e.g., unit tests that
 * invoke a filter directly). The {@link RejectionLogger#isEnabled() configured logger} treats a
 * {@code NONE} context as "do not log".
 *
 * @param ruleId          rspec rule id of the matcher (e.g., {@code "S6290"})
 * @param fileContext     input file being analyzed; {@code null} for {@link #NONE}
 * @param matchStartOffset start offset of the candidate inside the file; {@code -1} for {@link #NONE}
 */
public record RejectionLogContext(String ruleId, @Nullable InputFileContext fileContext, int matchStartOffset) {

  public static final RejectionLogContext NONE = new RejectionLogContext("", null, -1);

  public boolean isEmpty() {
    return fileContext == null || ruleId.isEmpty();
  }
}
