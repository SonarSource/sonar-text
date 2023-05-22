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
package org.sonar.plugins.secrets.configuration.deserialization;

import java.util.List;
import org.sonar.plugins.secrets.configuration.model.Provider;
import org.sonar.plugins.secrets.configuration.model.ProviderMetadata;
import org.sonar.plugins.secrets.configuration.model.Rule;
import org.sonar.plugins.secrets.configuration.model.RuleExample;
import org.sonar.plugins.secrets.configuration.model.RuleMetadata;
import org.sonar.plugins.secrets.configuration.model.Specification;
import org.sonar.plugins.secrets.configuration.model.modules.BooleanMatch;
import org.sonar.plugins.secrets.configuration.model.modules.Match;
import org.sonar.plugins.secrets.configuration.model.modules.MatchingType;
import org.sonar.plugins.secrets.configuration.model.modules.Modules;
import org.sonar.plugins.secrets.configuration.model.modules.PatternMatch;
import org.sonar.plugins.secrets.configuration.model.modules.PatternType;

public class ReferenceTestModel {

  public static Specification construct() {
    Specification specification = new Specification();
    specification.setProvider(constructProvider());
    specification.setRules(List.of(constructRule()));
    return specification;
  }

  public static Provider constructProvider() {
    Provider provider = new Provider();
    provider.setMetadata(constructProviderMetadata());
    return provider;
  }

  public static ProviderMetadata constructProviderMetadata() {
    ProviderMetadata providerMetadata = new ProviderMetadata();
    providerMetadata.setCategory("Cloud provider");
    providerMetadata.setMessage("Make sure that disclosing this AWS secret is safe here.");
    providerMetadata.setName("AWS");
    return providerMetadata;
  }

  public static Rule constructRule() {
    Rule rule = new Rule();
    rule.setId("aws-access-key");
    rule.setMetadata(constructRuleMetadata());
    rule.setModules(constructModules());
    rule.setExamples(List.of(constructRuleExample()));
    return rule;
  }

  public static RuleMetadata constructRuleMetadata() {
    RuleMetadata ruleMetadata = new RuleMetadata();
    ruleMetadata.setName("AWS access key");
    ruleMetadata.setCharset("[0-9a-z\\/+]");
    return ruleMetadata;
  }

  public static Modules constructModules() {
    Modules modules = new Modules();

    PatternMatch patternBefore = new PatternMatch();
    patternBefore.setType(PatternType.PATTERN_BEFORE);
    patternBefore.setPattern("AKIA[A-Z0-9]{16}");

    PatternMatch pattern = new PatternMatch();
    pattern.setType(PatternType.PATTERN);
    pattern.setPattern("[0-9a-z\\/+]{40}");
    List<Match> patternMatches = List.of(patternBefore, pattern);

    BooleanMatch booleanMatch = new BooleanMatch();
    booleanMatch.setType(MatchingType.MATCH_EITHER);
    booleanMatch.setModules(patternMatches);
    modules.setMatching(booleanMatch);
    return modules;
  }


  public static RuleExample constructRuleExample() {
    RuleExample example = new RuleExample();
    example.setContainsSecret(true);
    example.setText("example text\n");
    return example;
  }
}
