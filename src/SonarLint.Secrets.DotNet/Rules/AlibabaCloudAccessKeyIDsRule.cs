/*
 * Copyright (C) 2021 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */

// Ported from ...\sonar-secrets-plugin\src\main\java\com\sonarsource\secrets\rules\AlibabaCloudAccessKeyIDsRule.java

using SonarLint.Secrets.DotNet.Rules.Matching;
using System.ComponentModel.Composition;

namespace SonarLint.Secrets.DotNet.Rules
{
    [Export(typeof(ISecretDetector))]
    internal class AlibabaCloudAccessKeyIDsRule : AbstractAlibabaCloudAccessKeyRule
    {
        public AlibabaCloudAccessKeyIDsRule()

              : base("Make sure this Alibaba Cloud Access Key ID is not disclosed.",
                new RegexMatcher("(LTAI[0-9A-Za-z]{12}(:?[0-9A-Za-z]{8})?)\\b"))
        { }
    }
}
