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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.Checks;
import org.sonar.api.rule.RuleKey;
import org.sonar.plugins.text.api.CheckContext;
import org.sonar.plugins.text.api.CommonCheck;
import org.sonar.plugins.text.api.InitContext;
import org.sonar.plugins.text.core.InputFileContext;

public class ChecksVisitor {

  protected final List<BiConsumer<InputFileContext, InputFile>> consumers = new ArrayList<>();

  public ChecksVisitor(Checks<CommonCheck> checks) {
    Collection<CommonCheck> activeChecks = checks.all();
    for (CommonCheck check : activeChecks) {
      RuleKey ruleKey = checks.ruleKey(check);
      Objects.requireNonNull(ruleKey);
      check.initialize(new ContextAdapter(ruleKey));
    }
  }

  public void register(BiConsumer<InputFileContext, InputFile> visitor) {
    consumers.add(visitor);
  }

  public void scan(InputFileContext inputFileContext) {
    for(BiConsumer<InputFileContext, InputFile> consumer: consumers) {
      consumer.accept(inputFileContext, inputFileContext.inputFile);
    }
  }

  public class ContextAdapter implements CheckContext, InitContext {

    public final RuleKey ruleKey;
    private InputFileContext currentCtx;

    public ContextAdapter(RuleKey ruleKey) {
      this.ruleKey = ruleKey;
    }

    @Override
    public void reportLineIssue(int line, String message) {
      currentCtx.reportLineIssue(ruleKey, line, message);
    }

    @Override
    public void register(BiConsumer<CheckContext, InputFile> visitor) {
      ChecksVisitor.this.register((ctx, inputFile) -> {
        currentCtx = ctx;
        visitor.accept(this, inputFile);
      });
    }
  }
}
