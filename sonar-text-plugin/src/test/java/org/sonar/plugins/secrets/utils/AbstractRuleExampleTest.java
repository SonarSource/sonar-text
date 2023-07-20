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
package org.sonar.plugins.secrets.utils;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.function.Executable;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.batch.sensor.issue.Issue;
import org.sonar.plugins.common.Check;
import org.sonar.plugins.common.InputFileContext;
import org.sonar.plugins.secrets.api.Match;
import org.sonar.plugins.secrets.api.PatternMatcher;
import org.sonar.plugins.secrets.api.SpecificationBasedCheck;
import org.sonar.plugins.secrets.api.SpecificationLoader;
import org.sonar.plugins.secrets.configuration.model.Rule;
import org.sonar.plugins.secrets.configuration.model.RuleExample;
import org.sonar.plugins.secrets.configuration.model.matching.Matching;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.plugins.common.TestUtils.asString;
import static org.sonar.plugins.common.TestUtils.inputFile;
import static org.sonar.plugins.common.TestUtils.sensorContext;

public abstract class AbstractRuleExampleTest {
  private static SpecificationLoader specificationLoader;
  private final Check check;

  protected AbstractRuleExampleTest(Check check) {
    if (specificationLoader == null) {
      specificationLoader = new SpecificationLoader();
    }

    this.check = check;
    ((SpecificationBasedCheck) check).initialize(specificationLoader);
  }

  @TestFactory
  @DisplayName("Execute examples from the configuration file")
  Stream<DynamicTest> loadExamples() {
    List<Rule> rulesForKey = specificationLoader.getRulesForKey(check.ruleKey.rule());
    return rulesForKey.stream().flatMap(
      rule -> rule.getExamples().stream()
        // for easier debugging of specific rules uncomment the line below
        // .filter(example -> "<id-of-your-rule>".equals(rule.getId()))
        .map(example -> DynamicTest.dynamicTest(displayName(rule, example), analyzeExample(rule, example))));
  }

  private Executable analyzeExample(Rule rule, RuleExample ruleExample) {
    return () -> {
      SensorContextTester context = sensorContext(check);
      String exampleFileName = ruleExample.getFileName() != null ? ruleExample.getFileName() : "file.txt";
      InputFileContext inputFileContext = new InputFileContext(context, inputFile(Path.of(exampleFileName), ruleExample.getText()));

      check.analyze(inputFileContext);

      Collection<Issue> issues = context.allIssues();
      if (ruleExample.isContainsSecret()) {
        List<TextRange> expectedRanges = calculatePossibleRanges(ruleExample, inputFileContext);

        assertThat(issues).isNotEmpty();
        assertThat(issues).anyMatch(s -> asString(s).contains(rule.getMetadata().getMessage()));
        assertThat(issues).anyMatch(s -> asString(s).contains(rule.getRspecKey()));
        // Since there may be multiple occurrences of `ruleExample.match` in `ruleExample.text`, we check if
        // any of them matches the actual range.
        assertThat(issues).map(i -> i.primaryLocation().textRange()).containsAnyElementsOf(expectedRanges);
      } else {
        assertThat(issues).isEmpty();
      }
    };
  }

  private List<TextRange> calculatePossibleRanges(RuleExample ruleExample, InputFileContext ctx) {
    Matching matching = new Matching();
    // `Matcher#findIn` uses `Matcher#find`, so the pattern doesn't need to match the entire string
    matching.setPattern("(" + Pattern.quote(ruleExample.getMatch().stripTrailing()) + ")");
    PatternMatcher matcher = PatternMatcher.build(matching);
    List<Match> matches = matcher.findIn(ruleExample.getText());
    return matches.stream().map(m -> ctx.newTextRangeFromFileOffsets(m.getFileStartOffset(), m.getFileEndOffset())).collect(Collectors.toList());
  }

  private String displayName(Rule rule, RuleExample example) {
    String positiveOrNegative = example.isContainsSecret() ? "" : " not";
    int indexOfExampleInRule = rule.getExamples().indexOf(example) + 1;
    return String.format("%s example %d: Should%s find issue", rule.getId(), indexOfExampleInRule, positiveOrNegative);
  }

  public Check getInitializedCheck() {
    return check;
  }
}
