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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.check.Rule;
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
import org.sonarsource.analyzer.commons.regex.ast.FlagSet;
import org.sonarsource.analyzer.commons.regex.ast.RegexSyntaxElement;

public class TestRegexScannerContext implements JavaFileScannerContext, RegexScannerContext {

  private final List<Issue> issues = new ArrayList<>();
  private SecretsRegexTest.PatternLocation patternLocation;

  public void verify() {
    if (!issues.isEmpty()) {
      StringBuilder message = new StringBuilder(String.format("Found following issues in Regexes (%s):\n", issues.size()));
      for (Issue issue : issues) {
        var msg = String.format("%s, id: %s, \n\tLocation: %s, \n\tRegex: `%s` \n\tViolating rule %s: %s\n",
          issue.secretRuleKey,
          issue.secretRuleId,
          issue.location,
          issue.regexText,
          issue.issueRuleKey,
          issue.message);
        message.append(msg);
        if (!issue.details.isBlank()) {
          message.append(String.format("\t\t%s", issue.details));
        }
        message.append("\n");
      }
      throw new AssertionError(message.toString());
    }
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
    var part = regexText.substring(start, Math.min(end, regexText.length()));
    String details = String.format("Location: %s:%s text: %s", start, end, part);

    issues.add(new Issue(patternLocation.secretRspecKey, patternLocation.secretRuleId, patternLocation.location, ruleKey, regexText, message, details));
  }

  @Override
  public void reportIssue(RegexCheck regexCheck, Tree javaSyntaxElement, String message, @Nullable Integer cost, List<RegexCheck.RegexIssueLocation> secondaries) {
    var ruleKey = readRuleKey(regexCheck);
    issues.add(new Issue(patternLocation.secretRspecKey, patternLocation.secretRuleId, patternLocation.location, ruleKey, patternLocation.regex, message, ""));
  }

  @Override
  public RegexParseResult regexForLiterals(FlagSet initialFlags, LiteralTree... stringLiterals) {
    throw new RuntimeException("not expected to be called");
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

  static class Issue {
    String secretRuleKey;
    String secretRuleId;
    String location;
    String issueRuleKey;
    String regexText;
    String message;
    String details;

    public Issue(String secretRuleKey, String secretRuleId, String location, String issueRuleKey, String regexText, String message, String details) {
      this.secretRuleKey = secretRuleKey;
      this.secretRuleId = secretRuleId;
      this.location = location;
      this.issueRuleKey = issueRuleKey;
      this.regexText = regexText;
      this.message = message;
      this.details = details;
    }
  }
}
