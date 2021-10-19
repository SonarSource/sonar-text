/*
 * Copyright (C) 2021 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */

// Ported from ...\sonar-secrets-plugin\src\main\java\com\sonarsource\secrets\rules\AwsAccessKeyIdRule.java

using SonarLint.Secrets.DotNet.Rules.Matching;
using System.ComponentModel.Composition;

namespace SonarLint.Secrets.DotNet.Rules
{
    [Export(typeof(ISecretDetector))]
    internal class AwsAccessKeyIdRule : AbstractAwsRule
    {
        private const string NO_MATCH_SUFFIX = "EXAMPLE";

        public AwsAccessKeyIdRule()
              : base("Make sure this AWS Access Key ID is not disclosed.", new RegexMatcher("((?:AKIA|ASIA)[A-Z0-9]{16})\\b"))
        { }

        protected override bool isProbablyFalsePositive(string matchedText)
        {
            return matchedText.EndsWith(NO_MATCH_SUFFIX);
        }
    }

}
