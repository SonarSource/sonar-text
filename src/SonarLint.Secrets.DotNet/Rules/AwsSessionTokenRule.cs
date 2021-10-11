/*
 * Copyright (C) 2018-2021 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */

// Ported from ...\sonar-secrets-plugin\src\main\java\com\sonarsource\secrets\rules\AwsSessionTokenRule.java

using SonarLint.Secrets.DotNet.Rules.Matching;
using System.ComponentModel.Composition;

namespace SonarLint.Secrets.DotNet.Rules
{
    [Export(typeof(ISecretDetector))]
    internal class AwsSessionTokenRule : AbstractAwsRule
    {
        public AwsSessionTokenRule()

              : base("Make sure this AWS Session Token is not disclosed.",
                new RegexMatcher("(?i)session_?token.*?([0-9a-z\\/+=]{100,})"))
        { }
    }
}
