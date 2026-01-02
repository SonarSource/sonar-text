/*
 * SonarQube Text Plugin
 * Copyright (C) 2021-2026 SonarSource Sàrl
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
package org.sonar.plugins.common.analyzer;

import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.Version;
import org.sonar.plugins.common.InputFileContext;
import org.sonar.plugins.common.measures.DurationStatistics;
import org.sonar.plugins.secrets.api.SecretsSpecificationLoader;
import org.sonar.plugins.secrets.api.SpecificationBasedCheck;
import org.sonar.plugins.secrets.configuration.deserialization.SpecificationDeserializer;
import org.sonar.plugins.secrets.configuration.model.Specification;
import org.sonar.plugins.secrets.utils.CheckContainer;

import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.plugins.secrets.utils.SmileConverter.convertYamlToSmileStream;

@ExtendWith(MockitoExtension.class)
class TextAndSecretsAnalyzerTest {

  @Mock(answer = RETURNS_DEEP_STUBS)
  private SensorContext sensorContext;
  @Mock
  private DurationStatistics durationStatistics;
  @Mock
  private SecretsSpecificationLoader specLoader;
  @Mock
  private SpecificationBasedCheck checkWithPreFilter;
  @Mock
  private SpecificationBasedCheck checkWithoutPreFilter;
  @Mock
  private SpecificationBasedCheck checkWithMultiplePreFilters;
  @Mock
  private SpecificationBasedCheck checkWithOverlappingPreFilter;
  @Mock
  private RuleKey ruleKeyWithPreFilter;
  @Mock
  private RuleKey ruleKeyWithoutPreFilter;
  @Mock
  private RuleKey ruleKeyWithMultiplePreFilters;
  @Mock
  private RuleKey ruleKeyWithOverlappingPreFilter;

  private TextAndSecretsAnalyzer textAndSecretsAnalyzer;
  private Specification testSpec;

  @BeforeEach
  void setUp() {
    // language=YAML
    var yamlSpec = """
      provider:
        rules:
          - id: ruleWithPreFilter
            rspecKey: ruleWithPreFilter
            detection:
              pre:
                include:
                  content: [password]
          - id: ruleWithoutPreFilter
            rspecKey: ruleWithoutPreFilter
            detection:
              matching:
                pattern: ".*"
          - id: ruleWithMultiplePreFilters
            rspecKey: ruleWithMultiplePreFilters
            detection:
              pre:
                include:
                  content: [token, api, key, secret]
          - id: ruleWithOverlappingPreFilter
            rspecKey: ruleWithOverlappingPreFilter
            detection:
              pre:
                include:
                  content: [PASSWORD_ENV]
          - id: ruleWithDuplicatedPreFilters
            rspecKey: ruleWithDuplicatedPreFilters
            detection:
              pre:
                include:
                  content: [token]
      """;
    testSpec = SpecificationDeserializer.deserialize(convertYamlToSmileStream(yamlSpec), "test.sml");

    when(sensorContext.runtime().getApiVersion()).thenReturn(Version.create(12, 0));

    when(ruleKeyWithPreFilter.rule()).thenReturn("ruleWithPreFilter");
    when(ruleKeyWithoutPreFilter.rule()).thenReturn("ruleWithoutPreFilter");
    when(ruleKeyWithMultiplePreFilters.rule()).thenReturn("ruleWithMultiplePreFilters");
    when(checkWithPreFilter.getRuleKey()).thenReturn(ruleKeyWithPreFilter);
    when(checkWithoutPreFilter.getRuleKey()).thenReturn(ruleKeyWithoutPreFilter);
    when(checkWithMultiplePreFilters.getRuleKey()).thenReturn(ruleKeyWithMultiplePreFilters);

    when(specLoader.getRulesForKey("ruleWithPreFilter")).thenReturn(List.of(testSpec.getProvider().getRules().get(0)));
    when(specLoader.getRulesForKey("ruleWithoutPreFilter")).thenReturn(List.of(testSpec.getProvider().getRules().get(1)));
    when(specLoader.getRulesForKey("ruleWithMultiplePreFilters")).thenReturn(List.of(testSpec.getProvider().getRules().get(2)));

    when(durationStatistics.timed(anyString(), ArgumentMatchers.<Supplier<Object>>any())).thenAnswer(invocation -> {
      var supplier = invocation.getArgument(1, Supplier.class);
      return supplier.get();
    });
    lenient().doAnswer(invocation -> {
      var runnable = invocation.getArgument(1, Runnable.class);
      runnable.run();
      return null;
    }).when(durationStatistics).timed(anyString(), ArgumentMatchers.<Runnable>any());

    var checksContainer = new CheckContainer();
    checksContainer.initialize(List.of(checkWithPreFilter, checkWithoutPreFilter, checkWithMultiplePreFilters),
      specLoader, durationStatistics);

    textAndSecretsAnalyzer = new TextAndSecretsAnalyzer(
      sensorContext, mock(), durationStatistics,
      List.of(checkWithPreFilter, checkWithoutPreFilter, checkWithMultiplePreFilters),
      mock(), mock(), checksContainer);
  }

  @Test
  void shouldConstructAnalyzerWithPreFilteredChecks() {
    assertThat(textAndSecretsAnalyzer).isNotNull();
    verify(durationStatistics).timed(eq("trieBuild::general"), ArgumentMatchers.<Supplier>any());
  }

  @Test
  void shouldAnalyzeFileWithTrieMatches() {
    var inputFileContext = createMockInputFileContext("This contains password and secret tokens with api key");

    textAndSecretsAnalyzer.analyzeAllChecks(inputFileContext);

    verify(checkWithPreFilter, times(1)).analyze(inputFileContext);
    verify(checkWithMultiplePreFilters, times(1)).analyze(inputFileContext);
    verify(checkWithoutPreFilter, times(1)).analyze(inputFileContext);
    verify(inputFileContext, times(1)).flushIssues();
  }

  @Test
  void shouldAnalyzeFileWithoutTrieMatches() {
    var inputFileContext = createMockInputFileContext("This contains no matching content");

    textAndSecretsAnalyzer.analyzeAllChecks(inputFileContext);

    verify(checkWithPreFilter, never()).analyze(inputFileContext);
    verify(checkWithMultiplePreFilters, never()).analyze(inputFileContext);
    verify(checkWithoutPreFilter, times(1)).analyze(inputFileContext);
    verify(inputFileContext, times(1)).flushIssues();
  }

  @Test
  void shouldAnalyzeFileWithPartialTrieMatches() {
    var inputFileContext = createMockInputFileContext("This contains only password and some other text");

    textAndSecretsAnalyzer.analyzeAllChecks(inputFileContext);

    verify(checkWithPreFilter, times(1)).analyze(inputFileContext);
    verify(checkWithMultiplePreFilters, never()).analyze(inputFileContext);
    verify(checkWithoutPreFilter, times(1)).analyze(inputFileContext);
    verify(inputFileContext, times(1)).flushIssues();
  }

  @Test
  void shouldRunAllChecksEvenWithOverlappingPreFilters() {
    when(ruleKeyWithOverlappingPreFilter.rule()).thenReturn("ruleWithOverlappingPreFilter");
    when(checkWithOverlappingPreFilter.getRuleKey()).thenReturn(ruleKeyWithOverlappingPreFilter);
    when(specLoader.getRulesForKey("ruleWithOverlappingPreFilter")).thenReturn(List.of(testSpec.getProvider().getRules().get(3)));

    var checksContainer = new CheckContainer();
    checksContainer.initialize(List.of(checkWithPreFilter, checkWithoutPreFilter, checkWithMultiplePreFilters, checkWithOverlappingPreFilter),
      specLoader, durationStatistics);

    textAndSecretsAnalyzer = new TextAndSecretsAnalyzer(
      sensorContext, mock(), durationStatistics,
      List.of(checkWithPreFilter, checkWithoutPreFilter, checkWithMultiplePreFilters, checkWithOverlappingPreFilter),
      mock(), mock(), checksContainer);

    var inputFileContext = createMockInputFileContext("PASSWORD: ${{ password_env }}");

    textAndSecretsAnalyzer.analyzeAllChecks(inputFileContext);

    verify(checkWithPreFilter, times(1)).analyze(inputFileContext);
    verify(checkWithMultiplePreFilters, never()).analyze(inputFileContext);
    verify(checkWithOverlappingPreFilter, times(1)).analyze(inputFileContext);
    verify(checkWithoutPreFilter, times(1)).analyze(inputFileContext);
    verify(inputFileContext, times(1)).flushIssues();
  }

  @Test
  void shouldRunAllChecksEvenWithDuplicatedPreFilters() {
    var ruleKeyWithDuplicatedPreFilter = mock(RuleKey.class);
    var checkWithDuplicatedPreFilter = mock(SpecificationBasedCheck.class);
    when(ruleKeyWithDuplicatedPreFilter.rule()).thenReturn("ruleWithDuplicatedPreFilter");
    when(checkWithDuplicatedPreFilter.getRuleKey()).thenReturn(ruleKeyWithDuplicatedPreFilter);
    when(specLoader.getRulesForKey("ruleWithDuplicatedPreFilter")).thenReturn(List.of(testSpec.getProvider().getRules().get(4)));

    var checksContainer = new CheckContainer();
    checksContainer.initialize(List.of(checkWithMultiplePreFilters, checkWithDuplicatedPreFilter),
      specLoader, durationStatistics);

    textAndSecretsAnalyzer = new TextAndSecretsAnalyzer(
      sensorContext, mock(), durationStatistics,
      List.of(checkWithMultiplePreFilters, checkWithDuplicatedPreFilter),
      mock(), mock(), checksContainer);

    var inputFileContext = createMockInputFileContext("The word token should be captured by two checks");

    textAndSecretsAnalyzer.analyzeAllChecks(inputFileContext);

    verify(checkWithMultiplePreFilters, times(1)).analyze(inputFileContext);
    verify(checkWithDuplicatedPreFilter, times(1)).analyze(inputFileContext);
    verify(inputFileContext, times(1)).flushIssues();
  }

  @Test
  void shouldMatchCorrectlyWithMultithreading() throws InterruptedException {
    var inputFileContext1 = createMockInputFileContext(generateLongText("password"));
    var inputFileContext2 = createMockInputFileContext(generateLongText("TOKEN"));
    var inputFileContext3 = createMockInputFileContext(generateLongText("secret"));
    var inputFileContext4 = createMockInputFileContext(generateLongText("API_KEY"));

    var executor = newFixedThreadPool(4);
    List.of(inputFileContext1, inputFileContext2, inputFileContext3, inputFileContext4).forEach(context -> {
      executor.submit(() -> textAndSecretsAnalyzer.analyzeAllChecks(context));
    });

    executor.shutdown();
    var finished = executor.awaitTermination(20, SECONDS);
    assertThat(finished).as("Executor should finish analyzing files").isTrue();

    // Verify that no matches leak between threads by asserting that all checks are called only on expected files
    // (i.e. the files that contain the respective keywords).
    verify(checkWithPreFilter, never()).analyze(not(eq(inputFileContext1)));
    verify(checkWithoutPreFilter, times(1)).analyze(inputFileContext1);
    verify(checkWithoutPreFilter, times(1)).analyze(inputFileContext2);
    verify(checkWithoutPreFilter, times(1)).analyze(inputFileContext3);
    verify(checkWithoutPreFilter, times(1)).analyze(inputFileContext4);
    verify(checkWithMultiplePreFilters, never()).analyze(inputFileContext1);
    verify(checkWithMultiplePreFilters, times(1)).analyze(inputFileContext2);
    verify(checkWithMultiplePreFilters, times(1)).analyze(inputFileContext3);
    verify(checkWithMultiplePreFilters, times(1)).analyze(inputFileContext4);
    verify(checkWithOverlappingPreFilter, never()).analyze(not(eq(inputFileContext1)));
  }

  private static String generateLongText(String keyword) {
    var longText = new StringBuilder();
    var each = 100;
    for (int i = 0; i < 100_000; i++) {
      if (i % each == 0 && i > 0) {
        longText.append("Line with word: ").append(keyword).append("\n");
      }
      longText.append("Placeholder line without any specific words.\n");
    }
    return longText.toString();
  }

  private InputFileContext createMockInputFileContext(String content) {
    var inputFileContext = mock(InputFileContext.class);
    when(inputFileContext.content()).thenReturn(content);
    doNothing().when(inputFileContext).flushIssues();
    return inputFileContext;
  }
}
