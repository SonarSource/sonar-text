/*
 * SonarQube Text Plugin
 * Copyright (C) 2021-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
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

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIf;
import org.opentest4j.AssertionFailedError;
import org.sonar.api.internal.apachecommons.io.FileUtils;
import org.sonar.java.ast.parser.ArgumentListTreeImpl;
import org.sonar.java.ast.parser.FormalParametersListTreeImpl;
import org.sonar.java.ast.parser.QualifiedIdentifierListTreeImpl;
import org.sonar.java.checks.regex.AbstractRegexCheck;
import org.sonar.java.checks.regex.AbstractRegexCheckTrackingMatchers;
import org.sonar.java.checks.regex.AnchorPrecedenceCheck;
import org.sonar.java.checks.regex.CanonEqFlagInRegexCheck;
import org.sonar.java.checks.regex.DuplicatesInCharacterClassCheck;
import org.sonar.java.checks.regex.EmptyLineRegexCheck;
import org.sonar.java.checks.regex.EmptyRegexGroupCheck;
import org.sonar.java.checks.regex.EmptyStringRepetitionCheck;
import org.sonar.java.checks.regex.EscapeSequenceControlCharacterCheck;
import org.sonar.java.checks.regex.GraphemeClustersInClassesCheck;
import org.sonar.java.checks.regex.ImpossibleBackReferenceCheck;
import org.sonar.java.checks.regex.ImpossibleBoundariesCheck;
import org.sonar.java.checks.regex.InvalidRegexCheck;
import org.sonar.java.checks.regex.MultipleWhitespaceCheck;
import org.sonar.java.checks.regex.PossessiveQuantifierContinuationCheck;
import org.sonar.java.checks.regex.RedosCheck;
import org.sonar.java.checks.regex.RedundantRegexAlternativesCheck;
import org.sonar.java.checks.regex.RegexComplexityCheck;
import org.sonar.java.checks.regex.RegexLookaheadCheck;
import org.sonar.java.checks.regex.RegexStackOverflowCheck;
import org.sonar.java.checks.regex.ReluctantQuantifierCheck;
import org.sonar.java.checks.regex.ReluctantQuantifierWithEmptyContinuationCheck;
import org.sonar.java.checks.regex.SingleCharCharacterClassCheck;
import org.sonar.java.checks.regex.SingleCharacterAlternationCheck;
import org.sonar.java.checks.regex.SuperfluousCurlyBraceCheck;
import org.sonar.java.checks.regex.UnicodeCaseCheck;
import org.sonar.java.checks.regex.UnquantifiedNonCapturingGroupCheck;
import org.sonar.java.checks.regex.VerboseRegexCheck;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.model.JavaTree;
import org.sonar.java.model.declaration.ClassTreeImpl;
import org.sonar.java.model.declaration.MethodTreeImpl;
import org.sonar.java.model.declaration.ModifiersTreeImpl;
import org.sonar.java.model.declaration.VariableTreeImpl;
import org.sonar.java.model.expression.IdentifierTreeImpl;
import org.sonar.java.model.expression.LiteralTreeImpl;
import org.sonar.java.model.expression.MemberSelectExpressionTreeImpl;
import org.sonar.java.model.expression.MethodInvocationTreeImpl;
import org.sonar.java.model.statement.BlockTreeImpl;
import org.sonar.java.model.statement.IfStatementTreeImpl;
import org.sonar.java.regex.JavaAnalyzerRegexSource;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonar.plugins.secrets.api.SecretsSpecificationLoader;
import org.sonar.plugins.secrets.configuration.model.Rule;
import org.sonar.plugins.secrets.configuration.model.matching.AuxiliaryPattern;
import org.sonar.plugins.secrets.configuration.model.matching.BooleanCombination;
import org.sonar.plugins.secrets.configuration.model.matching.Match;
import org.sonarsource.analyzer.commons.regex.RegexParser;
import org.sonarsource.analyzer.commons.regex.ast.FlagSet;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public abstract class AbstractSecretsRegexTest {

  private final String specificationFilesLocation;
  private final Path configurationFilesPath;
  private final TestRegexScannerContext context;

  public AbstractSecretsRegexTest(String specificationFilesLocation, String baselineFileName) {
    this.specificationFilesLocation = specificationFilesLocation;
    this.context = new TestRegexScannerContext(baselineFileName);
    this.configurationFilesPath = Path.of("src/main/resources", specificationFilesLocation);
  }

  @Test
  void shouldValidateAllRegexesInConfiguration() {
    Set<String> listOfFileNames = listOfYamlFiles();

    List<PatternLocation> patternLocations = convertToPatternLocations(listOfFileNames);

    patternLocations.forEach(this::checkRegex);

    context.verify(true);
  }

  boolean disableValidateSingleFile() {
    return System.getProperty("filename") == null;
  }

  @Test
  @DisabledIf(value = "disableValidateSingleFile", disabledReason = "For simple run on single YAML file. It can be run from Gradle, see Readme.")
  void shouldValidateSingleFile() {
    var filename = System.getProperty("filename", "some-default-for-local-testing.yaml");
    List<PatternLocation> patternLocations = convertToPatternLocations(filename);

    patternLocations.forEach(this::checkRegex);

    context.verify(false);
  }

  @Test
  void shouldReportIssuesOnTestFile() {
    var specificationLoader = new SecretsSpecificationLoader("regex/", Set.of("specWithBadRegexes.yaml"));

    var patternLocations = specificationLoader.getRulesMappedToKey().entrySet().stream()
      .map(this::toPatternLocation)
      .flatMap(Collection::stream)
      .toList();

    patternLocations.forEach(this::checkRegex);

    assertThatThrownBy(() -> context.verify(false))
      .isInstanceOf(AssertionFailedError.class);
  }

  private Set<String> listOfYamlFiles() {
    String[] extensionsToSearchFor = new String[] {"yaml"};
    Collection<File> files = FileUtils.listFiles(new File(configurationFilesPath.toUri()), extensionsToSearchFor, false);
    return files.stream().map(File::getName).collect(Collectors.toSet());
  }

  private List<PatternLocation> convertToPatternLocations(String fileName) {
    return convertToPatternLocations(Set.of(fileName));
  }

  private List<PatternLocation> convertToPatternLocations(Set<String> listOfFileNames) {
    var specificationLoader = new SecretsSpecificationLoader(specificationFilesLocation, listOfFileNames);

    return specificationLoader.getRulesMappedToKey().entrySet().stream()
      .map(this::toPatternLocation)
      .flatMap(Collection::stream)
      .toList();
  }

  private List<PatternLocation> toPatternLocation(Map.Entry<String, List<Rule>> entry) {
    return entry.getValue().stream()
      .map(this::ruleToRegexes)
      .flatMap(Collection::stream)
      .toList();
  }

  private List<PatternLocation> ruleToRegexes(Rule rule) {
    String rspecKey = rule.getRspecKey();
    var secretRuleId = rule.getId();

    List<PatternLocation> patternLocations = new ArrayList<>();
    if (rule.getDetection().getMatching() != null) {
      patternLocations.add(new PatternLocation(
        rspecKey,
        secretRuleId,
        "matching",
        rule.getDetection().getMatching().getPattern().replace("\\", "\\\\")));
    }

    Match matchContext = rule.getDetection().getMatching().getContext();
    addMatchToRegexes(matchContext, patternLocations, rspecKey, secretRuleId);

    if (rule.getDetection().getPost() != null && rule.getDetection().getPost().getPatternNot() != null) {
      rule.getDetection().getPost().getPatternNot().stream()
        .map(pattern -> new PatternLocation(rspecKey, secretRuleId, "post", pattern.replace("\\", "\\\\")))
        .forEach(patternLocations::add);
    }

    return patternLocations;
  }

  private void addMatchToRegexes(Match match, List<PatternLocation> regexes, String rspecKey, String secretRuleId) {
    if (match instanceof AuxiliaryPattern auxiliaryPattern) {
      var p = new PatternLocation(rspecKey, secretRuleId, "auxiliary-" + auxiliaryPattern.getType().toString(), auxiliaryPattern.getPattern());
      regexes.add(p);
    }
    if (match instanceof BooleanCombination booleanCombination) {
      booleanCombination.getMatches().forEach(m -> addMatchToRegexes(m, regexes, rspecKey, secretRuleId));
    }
  }

  private void checkRegex(PatternLocation patternLocation) {
    var internalSyntaxToken = new InternalSyntaxToken(0, 0, "\"" + patternLocation.regex() + "\"", List.of(), false);
    LiteralTree stringLiteral = new LiteralTreeImpl(Tree.Kind.STRING_LITERAL, internalSyntaxToken);
    var regexTree = new JavaAnalyzerRegexSource(List.of(stringLiteral));
    var regexParser = new RegexParser(regexTree, new FlagSet());
    var parseResult = regexParser.parse();

    for (AbstractRegexCheck check : regexChecks()) {
      // Some checks (e.g. S5867) report an issue on Pattern.compile method and then the information about regex, location, etc. is missing
      context.setPatternLocation(patternLocation);
      check.setContext(context);

      MethodInvocationTree mit = methodInvocation(identifier("java.util.regex.Pattern"), identifier("compile"), List.of(stringLiteral));
      if (check instanceof AbstractRegexCheckTrackingMatchers) {
        // These checks raise issues after analyzing usages of regexes as well. That's why we need a more complex code snippet.
        check.checkRegex(parseResult, mit);
        check.leaveNode(buildJavaTree(patternLocation.regex()));
      } else {
        check.checkRegex(parseResult, mit);
      }
      check.leaveFile(context);
    }
  }

  private List<AbstractRegexCheck> regexChecks() {
    // * Disabled S4248 RegexPatternsNeedlesslyCheck Regex patterns should not be created needlessly - as patterns in sonar-text are
    // compiled once
    // * Disabled S5361 StringReplaceCheck "String#replace" should be preferred to "String#replaceAll" - doesn't make sense here as
    // Regexes are not used here to call replaceAll() method
    // * Disabled S5867 UnicodeAwareCharClassesCheck Unicode-aware versions of character classes should be preferred - is mostly
    // going to cause FPs and should not be enabled for secrets.
    // * Disabled S5860 UnusedGroupNamesCheck because named groups in secret specifications can exist for clarity.
    return List.of(
      new AnchorPrecedenceCheck(),
      new CanonEqFlagInRegexCheck(),
      new DuplicatesInCharacterClassCheck(),
      new EmptyLineRegexCheck(),
      new EmptyRegexGroupCheck(),
      new EmptyStringRepetitionCheck(),
      new EscapeSequenceControlCharacterCheck(),
      new GraphemeClustersInClassesCheck(),
      new ImpossibleBackReferenceCheck(),
      new ImpossibleBoundariesCheck(),
      new InvalidRegexCheck(),
      new MultipleWhitespaceCheck(),
      new PossessiveQuantifierContinuationCheck(),
      new RedosCheck(),
      new RedundantRegexAlternativesCheck(),
      new RegexComplexityCheck(),
      new RegexLookaheadCheck(),
      new RegexStackOverflowCheck(),
      new ReluctantQuantifierCheck(),
      new ReluctantQuantifierWithEmptyContinuationCheck(),
      new SingleCharCharacterClassCheck(),
      new SingleCharacterAlternationCheck(),
      new SuperfluousCurlyBraceCheck(),
      new UnicodeCaseCheck(),
      new UnquantifiedNonCapturingGroupCheck(),
      new VerboseRegexCheck());
  }

  /**
   * Build an AST for the following code snippet:
   * <pre>
   *   <code>class Example {
   *     void example(String input) {
   *       Pattern pattern = Pattern.compile([regex text]);
   *       Matcher matcher = pattern.matcher(input);
   *       if (matcher.matches()) {
   *       // EMPTY
   *       }
   *       if (matcher.find()) {
   *       // EMPTY
   *       }
   *     }
   *   }</code>
   * </pre>
   */
  private static CompilationUnitTree buildJavaTree(String regex) {
    var patternInitializer = methodInvocation(
      identifier("java.util.regex.Pattern"),
      identifier("compile"),
      List.of(new LiteralTreeImpl(Tree.Kind.STRING_LITERAL, internalSyntaxToken("\"" + regex + "\""))));
    var patternVariable = variableTree("pattern", patternInitializer);

    var matcherInitializer = methodInvocation(
      identifier("pattern"),
      identifier("matcher"),
      List.of(identifier("input")));
    var matcherVariable = variableTree("matcher", matcherInitializer);

    var matchesIfStatement = new IfStatementTreeImpl(internalSyntaxToken("if"), internalSyntaxToken("("),
      methodInvocation(identifier("matcher"), identifier("matches"), ArgumentListTreeImpl.emptyList()),
      internalSyntaxToken(")"), blockTree(List.of()), null, null);

    var findIfStatement = new IfStatementTreeImpl(internalSyntaxToken("if"), internalSyntaxToken("("),
      methodInvocation(identifier("matcher"), identifier("find"), ArgumentListTreeImpl.emptyList()),
      internalSyntaxToken(")"), blockTree(List.of()), null, null);

    var block = blockTree(List.of(patternVariable, matcherVariable, matchesIfStatement, findIfStatement));
    var methodTree = new MethodTreeImpl(
      new JavaTree.PrimitiveTypeTreeImpl(internalSyntaxToken("void")),
      identifier("example"),
      new FormalParametersListTreeImpl(internalSyntaxToken("("), internalSyntaxToken(")")),
      null,
      QualifiedIdentifierListTreeImpl.emptyList(),
      block,
      internalSyntaxToken(";"));

    var classTree = new ClassTreeImpl(Tree.Kind.CLASS, internalSyntaxToken("{"), List.of(methodTree), internalSyntaxToken("}"));
    return new JavaTree.CompilationUnitTreeImpl(null, List.of(), List.of(classTree), null, new InternalSyntaxToken(0, 0, "", List.of(), true));
  }

  private static InternalSyntaxToken internalSyntaxToken(String text) {
    return new InternalSyntaxToken(0, 0, text, List.of(), false);
  }

  private static IdentifierTree identifier(String text) {
    return new IdentifierTreeImpl(new InternalSyntaxToken(0, 0, text, List.of(), false));
  }

  private static VariableTree variableTree(String name, ExpressionTree initializer) {
    return new VariableTreeImpl(ModifiersTreeImpl.emptyModifiers(), identifier(name), initializer);
  }

  private static MethodInvocationTree methodInvocation(IdentifierTree receiver, IdentifierTree method, List<ExpressionTree> arguments) {
    var dot = new InternalSyntaxToken(0, 0, ".", List.of(), false);
    var argumentsTree = ArgumentListTreeImpl.emptyList();
    argumentsTree.addAll(arguments);
    return new MethodInvocationTreeImpl(
      new MemberSelectExpressionTreeImpl(receiver, dot, method),
      null, argumentsTree);
  }

  private static BlockTree blockTree(List<StatementTree> statements) {
    return new BlockTreeImpl(internalSyntaxToken("{"), statements, internalSyntaxToken("}"));
  }
}
