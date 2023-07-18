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
package org.sonar.plugins.secrets.api;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.plugins.common.Check;
import org.sonar.plugins.common.InputFileContext;
import org.sonar.plugins.secrets.SecretsRulesDefinition;
import org.sonar.plugins.secrets.configuration.model.Rule;

public abstract class SpecificationBasedCheck extends Check {

  private static final Logger LOG = Loggers.get(SpecificationBasedCheck.class);
  private List<SecretMatcher> matcher;

  @Override
  protected String repositoryKey() {
    return SecretsRulesDefinition.REPOSITORY_KEY;
  }

  protected SpecificationBasedCheck() {
    super();
  }

  public void initialize(SpecificationLoader loader) {
    List<Rule> rulesForKey = loader.getRulesForKey(ruleKey.rule());
    if (rulesForKey.isEmpty()) {
      LOG.error(String.format("Found no rule specification for rule with key: %s", ruleKey.rule()));
    }
    this.matcher = rulesForKey.stream()
      .map(SecretMatcher::build)
      .collect(Collectors.toList());
  }

  @Override
  public void analyze(InputFileContext ctx) {
    List<TextRange> foundSecrets = new ArrayList<>();

    for (SecretMatcher secretMatcher : matcher) {
      if (!secretMatcher.getPreFilter().test(ctx)) {
        continue;
      }

      secretMatcher.findIn(ctx.content()).stream()
        .map(match -> ctx.newTextRangeFromFileOffsets(match.getFileStartOffset(), match.getFileEndOffset()))
        .forEach(textRange -> {
          boolean notOverlapsExisting = foundSecrets.stream().noneMatch(foundSecret -> foundSecret.overlap(textRange));
          if (notOverlapsExisting) {
            foundSecrets.add(textRange);
            ctx.reportIssue(ruleKey, textRange, secretMatcher.getMessageFromRule());
          }
        });
    }
  }

  public List<SecretMatcher> getMatcher() {
    return matcher;
  }

}
