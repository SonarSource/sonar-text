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
import java.util.function.Predicate;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.plugins.common.Check;
import org.sonar.plugins.common.InputFileContext;
import org.sonar.plugins.secrets.SecretsRulesDefinition;
import org.sonar.plugins.secrets.configuration.model.Rule;

public abstract class SpecificationBasedCheck extends Check {

  private static final Logger LOG = Loggers.get(SpecificationBasedCheck.class);
  private Rule rule;
  private SecretsMatcher matcher;

  private Predicate<String> postFilter;

  @Override
  protected String repositoryKey() {
    return SecretsRulesDefinition.REPOSITORY_KEY;
  }

  protected SpecificationBasedCheck() {
    super();
  }

  public void initialize(SpecificationLoader loader) {
    this.rule = loader.getRuleForKey(ruleKey.rule());
    if (this.rule != null) {
      this.matcher = SecretsMatcherFactory.constructSecretsMatcher(rule);
      this.postFilter = PostFilterFactory.createPredicate(rule.getDetection().getPost());
    } else {
      LOG.error(String.format("Found no rule specification for rule with key: %s", ruleKey.rule()));
    }
  }

  @Override
  public void analyze(InputFileContext ctx) {
    List<TextRange> foundSecrets = new ArrayList<>();
    matcher.findIn(ctx.content()).stream()
      .filter(match -> postFilter.test(match.getText()))
      .map(match -> ctx.newTextRangeFromFileOffsets(match.getFileStartOffset(), match.getFileEndOffset()))
      .forEach(textRange -> {
        boolean notOverlapsExisting = foundSecrets.stream().noneMatch(foundSecret -> foundSecret.overlap(textRange));
        if (notOverlapsExisting) {
          foundSecrets.add(textRange);
          ctx.reportIssue(ruleKey, textRange, rule.getMetadata().getMessage());
        }
      });
  }

}
