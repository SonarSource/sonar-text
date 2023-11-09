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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIf;
import org.sonar.api.internal.apachecommons.io.FileUtils;
import org.sonar.java.ast.parser.ArgumentListTreeImpl;
import org.sonar.java.checks.regex.AbstractRegexCheck;
import org.sonar.java.checks.regex.AnchorPrecedenceCheck;
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
import org.sonar.java.checks.regex.UnusedGroupNamesCheck;
import org.sonar.java.checks.regex.VerboseRegexCheck;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.model.expression.IdentifierTreeImpl;
import org.sonar.java.model.expression.LiteralTreeImpl;
import org.sonar.java.model.expression.MemberSelectExpressionTreeImpl;
import org.sonar.java.model.expression.MethodInvocationTreeImpl;
import org.sonar.java.regex.JavaAnalyzerRegexSource;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.secrets.api.SpecificationLoader;
import org.sonar.plugins.secrets.configuration.model.Rule;
import org.sonar.plugins.secrets.configuration.model.matching.AuxiliaryPattern;
import org.sonar.plugins.secrets.configuration.model.matching.BooleanCombination;
import org.sonar.plugins.secrets.configuration.model.matching.Match;
import org.sonarsource.analyzer.commons.regex.RegexParser;
import org.sonarsource.analyzer.commons.regex.ast.FlagSet;

class SecretsRegexTest {

  private final static String CONFIGURATION_FILES_PATH = String.join(File.separator, "src", "main", "resources", "org", "sonar", "plugins", "secrets", "configuration");

  private final TestRegexScannerContext context = new TestRegexScannerContext();

  @Test
  void shouldValidateAllRegexesInConfiguration() {
    Set<String> listOfFileNames = listOfYamlFiles();

    List<PatternLocation> patternLocations = convertToPatternLocations(listOfFileNames);

    patternLocations.forEach(this::checkRegex);

    context.verify();
  }

  boolean disableValidateSingleFile() {
    return System.getProperty("filename") == null;
  }

  @Test
  @DisabledIf(value = "disableValidateSingleFile", disabledReason = "For simple run on single YAML file. It can be run from Maven, see Readme.")
  void shouldValidateSingleFile() {
    var filename = System.getProperty("filename", "some-default-for-local-testing.yaml");
    List<PatternLocation> patternLocations = convertToPatternLocations(filename);

    patternLocations.forEach(this::checkRegex);

    context.verify();
  }

  private static Set<String> listOfYamlFiles() {
    Path specificationsDirectory = Path.of(CONFIGURATION_FILES_PATH);
    String[] extensionsToSearchFor = new String[] {"yaml"};
    Collection<File> files = FileUtils.listFiles(new File(specificationsDirectory.toUri()), extensionsToSearchFor, false);
    return files.stream().map(File::getName).collect(Collectors.toSet());
  }

  private List<PatternLocation> convertToPatternLocations(String fileName) {
    return convertToPatternLocations(Set.of(fileName));
  }

  private List<PatternLocation> convertToPatternLocations(Set<String> listOfFileNames) {
    var specificationLoader = new SpecificationLoader(SpecificationLoader.DEFAULT_SPECIFICATION_LOCATION, listOfFileNames);

    return specificationLoader.getRulesMappedToKey().entrySet().stream()
      .map(this::toPatternLocation)
      .flatMap(Collection::stream)
      .collect(Collectors.toList());
  }

  private List<PatternLocation> toPatternLocation(Map.Entry<String, List<Rule>> entry) {
    return entry.getValue().stream()
      .map(this::ruleToRegexes)
      .flatMap(Collection::stream)
      .collect(Collectors.toList());
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

    Match context = rule.getDetection().getMatching().getContext();
    addMatchToRegexes(context, patternLocations, rspecKey, secretRuleId);

    if (rule.getDetection().getPost() != null && rule.getDetection().getPost().getPatternNot() != null) {
      rule.getDetection().getPost().getPatternNot().stream()
        .map(pattern -> new PatternLocation(rspecKey, secretRuleId, "post", pattern.replace("\\", "\\\\")))
        .forEach(patternLocations::add);
    }

    // TODO this method needs to be extended when implementing SONARTEXT-71 Implement negation for auxiliary patterns
    return patternLocations;
  }

  private void addMatchToRegexes(Match match, List<PatternLocation> regexes, String rspecKey, String secretRuleId) {
    if (match instanceof AuxiliaryPattern) {
      AuxiliaryPattern auxiliaryPattern = (AuxiliaryPattern) match;
      var p = new PatternLocation(rspecKey, secretRuleId, "auxiliary-" + auxiliaryPattern.getType().toString(), auxiliaryPattern.getPattern());
      regexes.add(p);
    }
    if (match instanceof BooleanCombination) {
      BooleanCombination booleanCombination = (BooleanCombination) match;
      booleanCombination.getMatches().forEach(m -> addMatchToRegexes(m, regexes, rspecKey, secretRuleId));
    }
  }

  private void checkRegex(PatternLocation patternLocation) {
    var internalSyntaxToken = new InternalSyntaxToken(0, 0, "\"" + patternLocation.regex + "\"", List.of(), false);
    LiteralTree stringLiteral = new LiteralTreeImpl(Tree.Kind.STRING_LITERAL, internalSyntaxToken);
    var regexTree = new JavaAnalyzerRegexSource(List.of(stringLiteral));
    var regexParser = new RegexParser(regexTree, new FlagSet());
    var parseResult = regexParser.parse();

    for (AbstractRegexCheck check : regexChecks()) {
      // Some checks (e.g. S5867) report an issue on Pattern.compile method and then the information about regex, location, etc. is missing
      context.setPatternLocation(patternLocation);
      check.setContext(context);

      ExpressionTree expr = new IdentifierTreeImpl(new InternalSyntaxToken(0, 0, "java.util.regex.Pattern", List.of(), false));
      ExpressionTree methodSelect = new MemberSelectExpressionTreeImpl(expr,
        new InternalSyntaxToken(0, 0, ".", List.of(), false),
        new IdentifierTreeImpl(new InternalSyntaxToken(0, 0, "compile", List.of(), false)));

      ArgumentListTreeImpl arguments = ArgumentListTreeImpl.emptyList();
      arguments.add(stringLiteral);
      MethodInvocationTree mit = new MethodInvocationTreeImpl(methodSelect, null, arguments);
      check.checkRegex(parseResult, mit);
    }
  }

  private List<AbstractRegexCheck> regexChecks() {
    // Disabled S4248 RegexPatternsNeedlesslyCheck Regex patterns should not be created needlessly
    // Disabled S5361 StringReplaceCheck "String#replace" should be preferred to "String#replaceAll"
    // Disabled S5854 CanonEqFlagInRegexCheck Regexes containing characters subject to normalization should use the CANON_EQ flag
    // Disabled S5867 UnicodeAwareCharClassesCheck Unicode-aware versions of character classes should be preferred
    return List.of(
      new AnchorPrecedenceCheck(),
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
      new UnusedGroupNamesCheck(),
      new VerboseRegexCheck());
  }

  static class PatternLocation {
    String secretRspecKey;
    String secretRuleId;
    String location;
    String regex;

    public PatternLocation(String secretRspecKey, String secretRuleId, String location, String regex) {
      this.secretRspecKey = secretRspecKey;
      this.secretRuleId = secretRuleId;
      this.location = location;
      this.regex = regex;
    }

  }
}
