/*
 * SonarQube Text Plugin
 * Copyright (C) 2021-2024 SonarSource SA
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.opentest4j.AssertionFailedError;
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.check.Rule;
import org.sonar.java.regex.JavaAnalyzerRegexSource;
import org.sonar.java.regex.RegexCheck;
import org.sonar.java.regex.RegexScannerContext;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.caching.CacheContext;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonarsource.analyzer.commons.regex.RegexParseResult;
import org.sonarsource.analyzer.commons.regex.RegexParser;
import org.sonarsource.analyzer.commons.regex.ast.FlagSet;
import org.sonarsource.analyzer.commons.regex.ast.RegexSyntaxElement;

public class TestRegexScannerContext implements JavaFileScannerContext, RegexScannerContext {

  private final List<Issue> issues = new ArrayList<>();
  private SecretsRegexTest.PatternLocation patternLocation;

  private SecretsRegexBaseline baseline;

  public TestRegexScannerContext() {
    try {
      var mapper = new ObjectMapper(new YAMLFactory());
      var file = new File("src/test/resources/SecretsRegexTest/baseline.yaml");
      var treeNode = mapper.readTree(file);
      baseline = mapper.treeToValue(treeNode, SecretsRegexBaseline.class);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void verify(boolean shouldWarnAboutUnusedIssuesInBaseline) {
    StringBuilder message = new StringBuilder();
    var numberOfNewIssues = 0;
    var issuesInUse = new HashSet<>();
    if (!issues.isEmpty()) {
      for (Issue issue : issues) {
        if (baseline.acceptedIssues().contains(issue)) {
          issuesInUse.add(issue);
          System.out.println("Found issue in baseline in acceptedIssues, ignored: " + issue);
        } else if (baseline.issuesToVerify().contains(issue)) {
          issuesInUse.add(issue);
          System.out.println("Found issue in baseline in issuesToVerify, ignored: " + issue);
        } else {
          var msg = toIssueMessage(issue);
          numberOfNewIssues++;
          message.append(msg);
          message.append("\n");
        }
      }
    }

    if (shouldWarnAboutUnusedIssuesInBaseline && baseline.acceptedIssues().size() + baseline.issuesToVerify().size() != issuesInUse.size()) {
      var unusedIssues = Stream.concat(baseline.acceptedIssues().stream(), baseline.issuesToVerify().stream())
        .filter(issue -> !issuesInUse.contains(issue))
        .map(TestRegexScannerContext::toIssueMessage)
        .collect(Collectors.joining("\n"));
      System.out.println("Found outdated issues in baseline (SecretsRegexTest/baseline.yaml), please clean up them! Outdated issues:\n" + unusedIssues);
    }
    if (numberOfNewIssues != 0) {
      var text = ("Found following issues in Regexes (%s):%n" +
        "Please fix them or suppress in SecretsRegexTest/baseline.yaml:%n%s")
          .formatted(numberOfNewIssues, message);
      throw new AssertionFailedError(text);
    }
  }

  private static String toIssueMessage(Issue issue) {
    return ("  - secretRuleKey: %s%n    secretRuleId: %s%n    location: %s%n    regexText: \"%s\"%n" +
      "    issueRuleKey: %s%n    message: %s%n    details: \"%s\"%n").formatted(
        issue.secretRuleKey,
        issue.secretRuleId,
        issue.location,
        issue.regexText.replace("\\\\", "\\\\\\\\").replace("\"", "\\\""),
        issue.issueRuleKey,
        issue.message,
        issue.details.replace("\\\\", "\\\\\\\\").replace("\"", "\\\""));
  }

  public void setPatternLocation(SecretsRegexTest.PatternLocation patternLocation) {
    this.patternLocation = patternLocation;
  }

  private String readRuleKey(RegexCheck regexCheck) {
    var annotation = regexCheck.getClass().getAnnotation(Rule.class);
    if (annotation != null) {
      return annotation.key();
    }
    return "unknown";
  }

  @Override
  public void reportIssue(RegexCheck regexCheck, RegexSyntaxElement regexSyntaxElement, String message, @Nullable Integer cost, List<RegexCheck.RegexIssueLocation> secondaries) {
    var regexText = regexSyntaxElement.getSource().getSourceText();
    var ruleKey = readRuleKey(regexCheck);
    var start = regexSyntaxElement.getRange().getBeginningOffset();
    var end = regexSyntaxElement.getRange().getEndingOffset();
    String details;
    if (start >= 0 && end >= 0) {
      var part = regexText.substring(start, Math.min(end, regexText.length()));
      details = String.format("Location: %s:%s text: %s", start, end, part);
    } else {
      // Some checks (e.g. RegexComplexityCheck) report on a strange range: opening quote only, with invalid indices. This is a quick workaround.
      details = "text: %s".formatted(regexText);
    }

    issues.add(new Issue(patternLocation.secretRspecKey, patternLocation.secretRuleId, patternLocation.location, ruleKey, regexText, message, details));
  }

  @Override
  public void reportIssue(RegexCheck regexCheck, Tree javaSyntaxElement, String message, @Nullable Integer cost, List<RegexCheck.RegexIssueLocation> secondaries) {
    var ruleKey = readRuleKey(regexCheck);
    issues.add(new Issue(patternLocation.secretRspecKey, patternLocation.secretRuleId, patternLocation.location, ruleKey, patternLocation.regex, message, ""));
  }

  @Override
  public RegexParseResult regexForLiterals(FlagSet initialFlags, LiteralTree... stringLiterals) {
    var regexTree = new JavaAnalyzerRegexSource(Arrays.asList(stringLiterals));
    var regexParser = new RegexParser(regexTree, new FlagSet());
    return regexParser.parse();
  }

  @Override
  public CompilationUnitTree getTree() {
    return null;
  }

  @Nullable
  @Override
  public Object getSemanticModel() {
    return null;
  }

  @Override
  public boolean fileParsed() {
    return false;
  }

  @Override
  public List<Tree> getComplexityNodes(Tree tree) {
    return null;
  }

  @Override
  public void reportIssue(JavaCheck javaCheck, Tree tree, String message) {
    throw new RuntimeException("not expected to be called");
  }

  @Override
  public void reportIssue(JavaCheck javaCheck, Tree tree, String message, List<Location> secondaryLocations, @Nullable Integer cost) {
    throw new RuntimeException("not expected to be called");
  }

  @Override
  public void reportIssueWithFlow(JavaCheck javaCheck, Tree tree, String message, Iterable<List<Location>> flows, @Nullable Integer cost) {
    throw new RuntimeException("not expected to be called");
  }

  @Override
  public void reportIssue(JavaCheck javaCheck, Tree startTree, Tree endTree, String message) {
    throw new RuntimeException("not expected to be called");
  }

  @Override
  public void reportIssue(JavaCheck javaCheck, Tree startTree, Tree endTree, String message, List<Location> secondaryLocations, @Nullable Integer cost) {
    throw new RuntimeException("not expected to be called");
  }

  @Override
  public List<String> getFileLines() {
    return null;
  }

  @Override
  public String getFileContent() {
    return null;
  }

  @Override
  public void addIssueOnFile(JavaCheck check, String message) {

  }

  @Override
  public void addIssue(int line, JavaCheck check, String message) {

  }

  @Override
  public void addIssue(int line, JavaCheck check, String message, @Nullable Integer cost) {

  }

  @Override
  public InputFile getInputFile() {
    return null;
  }

  @Override
  public void addIssueOnProject(JavaCheck check, String message) {

  }

  @Override
  public InputComponent getProject() {
    return null;
  }

  @Override
  public File getWorkingDirectory() {
    return null;
  }

  @Override
  public JavaVersion getJavaVersion() {
    return null;
  }

  @Override
  public boolean inAndroidContext() {
    return false;
  }

  @Override
  public CacheContext getCacheContext() {
    return null;
  }

  @Override
  public File getRootProjectWorkingDirectory() {
    return null;
  }

  @Override
  public String getModuleKey() {
    return null;
  }

  record Issue(String secretRuleKey,
    String secretRuleId,
    String location,
    String issueRuleKey,
    String regexText,
    String message,
    String details) {
  }
}
