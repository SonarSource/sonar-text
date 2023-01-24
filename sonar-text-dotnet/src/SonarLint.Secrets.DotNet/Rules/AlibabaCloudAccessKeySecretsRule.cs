/*
 * Copyright (C) 2021-2023 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */

// Ported from ...\sonar-secrets-plugin\src\main\java\com\sonarsource\secrets\rules\AlibabaCloudAccessKeySecretsRule.java

using SonarLint.Secrets.DotNet.Rules.Matching;
using System.ComponentModel.Composition;

namespace SonarLint.Secrets.DotNet.Rules
{
    [Export(typeof(ISecretDetector))]
    internal class AlibabaCloudAccessKeySecretsRule : AbstractAlibabaCloudAccessKeyRule
    {
        public AlibabaCloudAccessKeySecretsRule()
        : base("Make sure this Alibaba Cloud Access Key Secret is not disclosed.",
          new RegexMatcher("(?i)ali(?:yun|baba|cloud).{0,50}['\"`]([0-9a-z]{30})['\"`]"),
          new RegexMatcher("(?i)(?:SECRET_?(?:ACCESS)?_?KEY|(?:ACCESS)?_?KEY_?SECRET)\\b[^0-9a-z]{0,10}([0-9a-z]{30})[^a-z0-9\\/+=$\\-_]"))
        {
        }

        protected override bool IsProbablyFalsePositive(string matchedText) =>
            EntropyChecker.HasLowEntropy(matchedText);
    }
}
