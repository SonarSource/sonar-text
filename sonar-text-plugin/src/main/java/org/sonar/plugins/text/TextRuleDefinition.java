/*
 * SonarQube Text Plugin
 * Copyright (C) 2021-2025 SonarSource Sàrl
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
package org.sonar.plugins.text;

import java.util.List;
import org.sonar.api.SonarRuntime;
import org.sonar.plugins.common.CommonRulesDefinition;
import org.sonar.plugins.common.DefaultQualityProfileDefinition;

public class TextRuleDefinition extends CommonRulesDefinition {

  public static final String REPOSITORY_KEY = "text";
  public static final String REPOSITORY_NAME = "Sonar";

  public TextRuleDefinition(SonarRuntime sonarRuntime) {
    super(sonarRuntime, REPOSITORY_KEY, REPOSITORY_NAME, TextLanguage.KEY);
  }

  public static class DefaultQualityProfile extends DefaultQualityProfileDefinition {
    public DefaultQualityProfile() {
      super(REPOSITORY_KEY, TextLanguage.KEY);
    }
  }

  @Override
  public List<Class<?>> checks() {
    return new TextCheckList().checks();
  }

}
