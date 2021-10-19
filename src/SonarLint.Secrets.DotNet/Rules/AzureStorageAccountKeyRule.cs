/*
 * Copyright (C) 2021 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */

// Ported from ...\sonar-secrets-plugin\src\main\java\com\sonarsource\secrets\rules\AzureStorageAccountKeyRule.java

using SonarLint.Secrets.DotNet.Rules.Matching;
using System.ComponentModel.Composition;

namespace SonarLint.Secrets.DotNet.Rules
{
    [Export(typeof(ISecretDetector))]
    internal class AzureStorageAccountKeyRule : AbstractSecretRule
    {
        public AzureStorageAccountKeyRule()

              : base("S6338", "Azure Storage Account Keys should not be disclosed",
                "Make sure this Azure Storage Account Key is not disclosed.",
                new ConditionalMatcher(
                    content => content.Contains("core.windows.net"),
                    new RegexMatcher("['\"`]([a-zA-Z0-9/\\+]{86}==)['\"`]")),
                new RegexMatcher("AccountKey=([a-zA-Z0-9/\\+]{86}==)"))
        { }
    }
}
