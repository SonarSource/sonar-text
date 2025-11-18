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
package org.sonar.plugins.secrets.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;
import com.networknt.schema.Error;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.extension.TestInstantiationException;
import org.junit.jupiter.api.function.Executable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.sensor.issue.Issue;
import org.sonar.plugins.common.InputFileContext;
import org.sonar.plugins.secrets.api.PatternMatcher;
import org.sonar.plugins.secrets.api.SecretsSpecificationLoader;
import org.sonar.plugins.secrets.api.SpecificationBasedCheck;
import org.sonar.plugins.secrets.api.SpecificationConfiguration;
import org.sonar.plugins.secrets.configuration.model.Rule;
import org.sonar.plugins.secrets.configuration.model.RuleExample;
import org.sonar.plugins.secrets.configuration.model.matching.Matching;
import org.sonar.plugins.secrets.configuration.validation.SchemaValidator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.plugins.common.TestUtils.asString;
import static org.sonar.plugins.common.TestUtils.inputFile;
import static org.sonar.plugins.common.TestUtils.mockDurationStatistics;
import static org.sonar.plugins.common.TestUtils.sensorContext;
import static org.sonar.plugins.secrets.SecretsSpecificationFilesDefinition.existingSecretSpecifications;

public abstract class AbstractRuleExampleTest {

  private static final Logger LOG = LoggerFactory.getLogger(AbstractRuleExampleTest.class);
  private static SecretsSpecificationLoader specificationLoader;
  private final SpecificationBasedCheck check;
  private final String specificationLocation;
  private final Collection<Throwable> loaderExceptions = new HashSet<>();

  protected AbstractRuleExampleTest(SpecificationBasedCheck check) {
    this(check, SecretsSpecificationLoader.DEFAULT_SPECIFICATION_LOCATION, existingSecretSpecifications());
  }

  protected AbstractRuleExampleTest(SpecificationBasedCheck check, String specificationLocation, Set<String> specifications) {
    this.specificationLocation = specificationLocation;
    if (specificationLoader == null) {
      specificationLoader = new SecretsSpecificationLoader(specificationLocation, specifications, (e, ignored) -> loaderExceptions.add(e));
    }

    this.check = check;
    check.initialize(specificationLoader, mockDurationStatistics(), SpecificationConfiguration.AUTO_TEST_FILE_DETECTION_ENABLED);

    if (!loaderExceptions.isEmpty()) {
      for (Throwable e : loaderExceptions) {
        LOG.error("Exception loading specification", e);
      }
      throw new TestInstantiationException("Failed to load specification", loaderExceptions.iterator().next());
    }
  }

  @TestFactory
  @DisplayName("Execute examples from the configuration file")
  Stream<DynamicTest> loadExamples() {
    var rulesForKey = specificationLoader.getRulesForKey(check.getRuleKey().rule());
    return rulesForKey.stream().flatMap(
      rule -> rule.getExamples().stream()
        // for easier debugging of specific rules uncomment the line below
        // .filter(example -> "<id-of-your-rule>".equals(rule.getId()))
        .map(example -> DynamicTest.dynamicTest(displayName(rule, example), analyzeExample(rule, example))));
  }

  @Test
  void testSpecificationFileValidity() throws IOException {
    var mapper = new ObjectMapper(new SmileFactory());
    var fileNames = specificationLoader.getSpecificationFilesForKey(check.getRuleKey().rule());
    for (String fileName : fileNames) {
      var specificationStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(specificationLocation + fileName);
      var specification = mapper.readTree(specificationStream);

      var validationMessages = SchemaValidator.validate(specification);
      assertThat(validationMessages)
        .as(validationMessageReport(fileName, validationMessages))
        .isEmpty();
    }
  }

  Supplier<String> validationMessageReport(String fileName, List<Error> validationMessages) {
    return () -> {
      StringBuilder sb = new StringBuilder("file '%s' has %d validation errors:".formatted(fileName, validationMessages.size()));
      for (var validationMessage : validationMessages) {
        sb.append("\n- ").append(validationMessage.getMessage());
      }
      return sb.toString();
    };
  }

  private Executable analyzeExample(Rule rule, RuleExample ruleExample) {
    return () -> {
      var context = sensorContext(check);
      var exampleFileName = ruleExample.getFileName() != null ? ruleExample.getFileName() : "file.txt";
      var inputFileContext = new InputFileContext(context, inputFile(Path.of(exampleFileName), ruleExample.getText()));

      var checksContainer = new CheckContainer();
      checksContainer.initialize(Set.of(check), specificationLoader, mockDurationStatistics());
      checksContainer.analyze(inputFileContext, rule.getId());

      var issues = context.allIssues();
      if (ruleExample.isContainsSecret()) {
        List<TextRange> expectedRanges = calculatePossibleRanges(ruleExample, inputFileContext, rule.getId());

        assertThat(issues).withFailMessage(() -> failingMessage(issues, rule, ruleExample)).isNotEmpty();
        assertThat(issues).anyMatch(s -> asString(s).contains(rule.getMetadata().getMessage()));
        assertThat(issues).anyMatch(s -> asString(s).contains(rule.getRspecKey()));
        // Since there may be multiple occurrences of `ruleExample.match` in `ruleExample.text`, we check if
        // any of them matches the actual range.
        assertThat(expectedRanges).withFailMessage(() -> failingMessage(issues, rule, ruleExample)).isNotEmpty();
        assertThat(issues).map(i -> i.primaryLocation().textRange())
          .withFailMessage(() -> failingMessage(issues, rule, ruleExample, expectedRanges))
          .containsAnyElementsOf(expectedRanges);
      } else {
        assertThat(issues).withFailMessage(() -> failingMessage(issues, rule, ruleExample)).isEmpty();
      }
    };
  }

  private List<TextRange> calculatePossibleRanges(RuleExample ruleExample, InputFileContext ctx, String ruleId) {
    var matching = new Matching();
    // `Matcher#findIn` uses `Matcher#find`, so the pattern doesn't need to match the entire string
    matching.setPattern("(" + Pattern.quote(ruleExample.getMatch().stripTrailing()) + ")");
    var matcher = PatternMatcher.build(matching);
    var matches = matcher.findIn(ruleExample.getText(), ruleId);
    return matches.stream().map(m -> ctx.newTextRangeFromFileOffsets(m.fileStartOffset(), m.fileEndOffset())).toList();
  }

  private String displayName(Rule rule, RuleExample example) {
    var positiveOrNegative = example.isContainsSecret() ? "" : " not";
    var indexOfExampleInRule = rule.getExamples().indexOf(example) + 1;
    return String.format("%s example %d: Should%s find issue", rule.getId(), indexOfExampleInRule, positiveOrNegative);
  }

  private String failingMessage(Collection<Issue> issues, Rule rule, RuleExample ruleExample) {
    return failingMessage(issues, rule, ruleExample, List.of());
  }

  private String failingMessage(Collection<Issue> issues, Rule rule, RuleExample ruleExample, List<TextRange> expectedRanges) {
    var sb = new StringBuilder();
    sb.append("Test case \"");
    sb.append(displayName(rule, ruleExample));
    if (issues.isEmpty()) {
      sb.append("\" failed as it didn't match on any content");
      return sb.toString();
    }

    sb.append("\" failed as it matched on the following content:");
    sb.append(System.lineSeparator());
    for (Issue issue : issues) {
      sb.append("- \"");
      try {
        sb.append(retrieveMatchedContent(issue, ruleExample.getText()));
      } catch (NullPointerException | IndexOutOfBoundsException e) {
        sb.append("<Could not calculate matched content cause of ");
        sb.append(e.getClass().getSimpleName());
        sb.append(">");
        LOG.error("Exception calculating matched content, see test results below", e);
      }
      sb.append("\"");
      sb.append("    (").append(issue.primaryLocation().textRange()).append(")");
      sb.append(System.lineSeparator());
    }

    if (ruleExample.isContainsSecret()) {
      sb.append("But the example should have matched on:");
      sb.append(System.lineSeparator());
      sb.append("- \"");
      sb.append(ruleExample.getMatch());
      sb.append("\"");
      if (!ruleExample.getText().contains(ruleExample.getMatch()))
        sb.append("    (not in the example text)");
      else if (expectedRanges.isEmpty()) {
        sb.append("    (unknown range)");
      } else {
        sb.append("    (");
        sb.append(expectedRanges.stream().map(Object::toString).collect(Collectors.joining(" or ")));
        sb.append(")");
      }
    } else {
      sb.append("But the example doesn't expect a match");
    }
    return sb.toString();
  }

  private String retrieveMatchedContent(Issue issue, String content) {
    var split = content.split("\\R");
    var sb = new StringBuilder();
    var startLine = issue.primaryLocation().textRange().start().line();
    var endLine = issue.primaryLocation().textRange().end().line();
    for (int i = startLine; i <= endLine; i++) {
      String stringToAdd = split[i - 1];
      if (i == endLine) {
        stringToAdd = stringToAdd.substring(0, issue.primaryLocation().textRange().end().lineOffset());
      }
      if (i == startLine) {
        stringToAdd = stringToAdd.substring(issue.primaryLocation().textRange().start().lineOffset());
      }
      sb.append(stringToAdd);
    }
    return sb.toString();
  }
}
