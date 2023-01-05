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
package org.sonar.plugins.secrets.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.plugins.common.Check;
import org.sonar.plugins.common.InputFileContext;

public class SecretRule {

  private final String message;
  private final List<SecretsMatcher> matchers;
  private final Predicate<String> isMatchedTextFalsePositive;

  public SecretRule(String message, SecretsMatcher... matchers) {
    this(message, text -> false, matchers);
  }

  public SecretRule(String message, Predicate<String> isMatchedTextFalsePositive, SecretsMatcher... matchers) {
    this.message = message;
    this.matchers = Arrays.asList(matchers);
    this.isMatchedTextFalsePositive = isMatchedTextFalsePositive;
  }

  public final void analyze(Check check, InputFileContext ctx) {
    List<TextRange> foundSecrets = new ArrayList<>();
    matchers.stream()
      .flatMap(matcher -> matcher.findIn(ctx.content()).stream())
      .filter(match -> !isMatchedTextFalsePositive.test(match.getText()))
      .map(match -> ctx.newTextRangeFromFileOffsets(match.getFileStartOffset(), match.getFileEndOffset()))
      .forEach(textRange -> {
        boolean notOverlapsExisting = foundSecrets.stream().noneMatch(foundSecret -> foundSecret.overlap(textRange));
        if (notOverlapsExisting) {
          foundSecrets.add(textRange);
          ctx.reportIssue(check.ruleKey, textRange, message);
        }
      });
  }

}
