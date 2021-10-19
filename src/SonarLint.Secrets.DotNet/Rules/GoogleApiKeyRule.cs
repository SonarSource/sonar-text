/*
 * Copyright (C) 2021 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */

// Ported from ...\sonar-secrets-plugin\src\main\java\com\sonarsource\secrets\rules\GoogleApiKeyRule.java

using SonarLint.Secrets.DotNet.Rules.Matching;
using System.ComponentModel.Composition;

namespace SonarLint.Secrets.DotNet.Rules
{
    [Export(typeof(ISecretDetector))]
    [PartCreationPolicy(CreationPolicy.Shared)]
    internal class GoogleApiKeyRule : AbstractSecretRule
    {
        public GoogleApiKeyRule()
              : base("S6334", "Google API keys should not be disclosed",
                "Make sure this Google API Key is not disclosed.",
                new RegexMatcher("(AIza[0-9A-Za-z\\-_]{35})\\b")) { }
    }
}
