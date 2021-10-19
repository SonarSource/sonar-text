/*
 * Copyright (C) 2021 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */

// Ported from ...\sonar-secrets-plugin\src\main\java\com\sonarsource\secrets\rules\AwsAccessKeyRule.java

using SonarLint.Secrets.DotNet.Rules.Matching;
using System.ComponentModel.Composition;

namespace SonarLint.Secrets.DotNet.Rules
{
    [Export(typeof(ISecretDetector))]
    internal class AwsAccessKeyRule : AbstractAwsRule
    {
        private readonly EntropyChecker entropyChecker;

        public AwsAccessKeyRule()

              : base("Make sure this AWS Secret Access Key is not disclosed.",
                new RegexMatcher("(?i)aws.{0,50}['\"`]([0-9a-z\\/+]{40})['\"`]"),
                new RegexMatcher("(?i)\\b(?:AWS)?_?SECRET_?(?:ACCESS)?_?KEY\\b.{0,10}([0-9a-z\\/+]{40})"))
        {
            entropyChecker = new EntropyChecker();
        }

        protected override bool isProbablyFalsePositive(string matchedText)
        {
            return entropyChecker.hasLowEntropy(matchedText);
        }
    }
}
