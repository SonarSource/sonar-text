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

import java.util.List;
import org.sonar.check.Rule;
import org.sonar.plugins.common.InputFileContext;
import org.sonar.plugins.text.api.AbstractUnicodeSequenceCheck;

@Rule(key = "S8522")
public class VariationSelectorCheck extends AbstractUnicodeSequenceCheck {

  public static final String MESSAGE_FORMAT = "This line contains %d consecutive Variation Selector characters starting at column %d. " +
    "Make sure that using consecutive variation selectors is intentional and safe here.";

  @Override
  protected boolean isSequenceChar(int codePoint) {
    // Variation Selectors Supplement (U+FE00–U+FE0F)
    // Variation Selectors Supplement (U+E0100–U+E01EF), used in the Glassworm supply chain attack
    return (codePoint >= 0xFE00 && codePoint <= 0xFE0F) ||
      (codePoint >= 0xE0100 && codePoint <= 0xE01EF);
  }

  @Override
  protected void reportSequence(InputFileContext ctx, int lineNumber, int startColumn, List<Integer> sequenceChars) {
    ctx.reportTextIssue(getRuleKey(), lineNumber, MESSAGE_FORMAT.formatted(sequenceChars.size(), startColumn));
  }

  @Override
  protected int minSequenceLength() {
    // 2 or 3 consecutive variation selectors can often appear as artifacts of copy-paste and shouldn't hold any malicious payload
    return 4;
  }
}
