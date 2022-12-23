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

import org.sonar.plugins.secrets.rules.matching.SecretsMatcher;
import org.sonar.plugins.secrets.NormalizedInputFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.sonar.api.batch.fs.TextRange;

public abstract class AbstractSecretRule implements SecretRule {

  private final String ruleKey;
  private final String message;
  private final List<SecretsMatcher> matchers;

  protected AbstractSecretRule(String ruleKey, String message, SecretsMatcher... matchers) {
    this.ruleKey = ruleKey;
    this.message = message;
    this.matchers = Arrays.asList(matchers);
  }

  @Override
  public String getRuleKey() {
    return ruleKey;
  }
  
  @Override
  public String getMessage() {
    return message;
  }

  protected boolean isProbablyFalsePositive(String matchedText) {
    return false;
  }

  @Override
  public List<Secret> findSecretsIn(NormalizedInputFile inputFile) {
    List<Secret> foundSecrets = new ArrayList<>();
    matchers.stream()
      .flatMap(matcher -> matcher.findIn(inputFile.getContent()).stream())
      .filter(match -> !isProbablyFalsePositive(match.getText()))
      .map(match -> inputFile.newTextRangeFromFileOffsets(match.getFileStartOffset(), match.getFileEndOffset()))
      .forEach(textRange -> {
        if (noneMatchedRangeOverlaps(foundSecrets, textRange)) {
          foundSecrets.add(new Secret(textRange));
        }
      });
    return foundSecrets;
  }

  private static boolean noneMatchedRangeOverlaps(List<Secret> foundSecrets, TextRange newRange) {
    return foundSecrets.stream().noneMatch(foundSecret -> foundSecret.getTextRange().overlap(newRange));
  }
}
