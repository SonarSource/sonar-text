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
package org.sonar.plugins.text.visitor;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.sonar.api.batch.rule.Checks;
import org.sonar.api.rule.RuleKey;
import org.sonar.plugins.text.api.CheckContext;
import org.sonar.plugins.text.api.TextCheck;
import org.sonar.plugins.text.core.InputFileContext;

public class ChecksVisitor {

  private final Set<ContextAdapter> contextAdapters = new HashSet<>();
  private final Collection<TextCheck> activeChecks;

  public ChecksVisitor(Checks<TextCheck> checks) {
    activeChecks = checks.all();
    for (TextCheck check : activeChecks) {
      RuleKey ruleKey = checks.ruleKey(check);
      Objects.requireNonNull(ruleKey);
      ContextAdapter contextAdapter = new ContextAdapter(ruleKey);
      contextAdapters.add(contextAdapter);
      check.initialize(contextAdapter);
    }
  }

  public void scan(InputFileContext inputFileContext) {
    contextAdapters.forEach(ctx -> ctx.currentCtx = inputFileContext);
    activeChecks.forEach(check -> check.analyze(inputFileContext.inputFile));
  }

  public static class ContextAdapter implements CheckContext {

    public final RuleKey ruleKey;
    private InputFileContext currentCtx;

    public ContextAdapter(RuleKey ruleKey) {
      this.ruleKey = ruleKey;
    }

    @Override
    public void reportLineIssue(int line, String message) {
      currentCtx.reportLineIssue(ruleKey, line, message);
    }

  }
}
