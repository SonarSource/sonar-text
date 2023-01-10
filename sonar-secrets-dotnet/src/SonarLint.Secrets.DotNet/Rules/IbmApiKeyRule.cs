/*
 * Copyright (C) 2021-2022 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */

// Ported from ...\sonar-secrets-plugin\src\main\java\com\sonarsource\secrets\rules\IbmApiKeyRule.java

using SonarLint.Secrets.DotNet.Rules.Matching;
using System.ComponentModel.Composition;

namespace SonarLint.Secrets.DotNet.Rules
{
    [Export(typeof(ISecretDetector))]
    [PartCreationPolicy(CreationPolicy.Shared)]
    internal class IbmApiKeyRule : AbstractSecretRule
    {
        public IbmApiKeyRule()
              : base("S6337", "IBM API keys should not be disclosed",
                "Make sure this IBM API key is not disclosed.",
                new RegexMatcher("(?is)(?:ibm|apikey).{0,50}['\"`]([a-z0-9_\\-]{44})['\"`]"))
        {
        }

        protected override bool IsProbablyFalsePositive(string matchedText) =>
            EntropyChecker.HasLowEntropy(matchedText);
    }
}
