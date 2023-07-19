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

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.assertj.core.data.MapEntry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.function.ThrowingConsumer;
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

import static org.sonar.plugins.common.TestUtils.asString;
import static org.sonar.plugins.common.TestUtils.inputFile;
import static org.sonar.plugins.common.TestUtils.sensorContext;

public abstract class AbstractRuleExampleTest {
    private final Check check;
    private final SpecificationLoader specificationLoader;
    private final String rspecKey;

    protected AbstractRuleExampleTest(Check check, String rspecKey) {
        this.rspecKey = rspecKey;
        specificationLoader = new SpecificationLoader();
        this.check = check;
        ((SpecificationBasedCheck) check).initialize(specificationLoader);
    }

    @TestFactory
    @DisplayName("Execute examples from the configuration file")
    Stream<DynamicTest> loadExamples() {
        Stream<MapEntry<Rule, RuleExample>> input = specificationLoader.getAllRules().stream()
                .flatMap(rule -> rule.getExamples().stream().map(e -> MapEntry.entry(rule, e)));
        Function<MapEntry<Rule, RuleExample>, String> displayNameGenerator = (pair) ->
                pair.getKey().getId() + " " + pair.getKey().getMetadata().getName() + " (" + (pair.getValue().isContainsSecret() ? "positive" : "negative") + ")";
        ThrowingConsumer<MapEntry<Rule, RuleExample>> testExecutor = ruleToExample -> checkExample(ruleToExample.getKey(), ruleToExample.getValue());

        return input
                .filter(e -> rspecKey.equals(e.getKey().getRspecKey()))
                .map(e -> DynamicTest.dynamicTest(displayNameGenerator.apply(e), () -> testExecutor.accept(e)));
    }

    private void checkExample(Rule rule, RuleExample ruleExample) throws IOException {
        SensorContextTester context = sensorContext(check);
        InputFileContext inputFileContext = new InputFileContext(context, inputFile(ruleExample.getText()));

        check.analyze(inputFileContext);

        Collection<Issue> issues = context.allIssues();
        if (ruleExample.isContainsSecret()) {
            Matching matching = new Matching();
            matching.setPattern(".*(" + Pattern.quote(ruleExample.getMatch().stripTrailing()) + ").*");
            PatternMatcher matcher = PatternMatcher.build(matching);
            List<Match> matches = matcher.findIn(ruleExample.getText());
            TextRange expectedRange = inputFileContext.newTextRangeFromFileOffsets(matches.get(0).getFileStartOffset(), matches.get(0).getFileEndOffset());

            Assertions.assertThat(issues).isNotEmpty();
            Assertions.assertThat(issues).anyMatch(s -> asString(s).contains(rule.getMetadata().getMessage()));
            Assertions.assertThat(issues).anyMatch(s -> asString(s).contains(rule.getRspecKey()));
            Assertions.assertThat(issues).map(i -> i.primaryLocation().textRange()).contains(expectedRange);
        } else {
            Assertions.assertThat(issues).isEmpty();
        }
    }
}
