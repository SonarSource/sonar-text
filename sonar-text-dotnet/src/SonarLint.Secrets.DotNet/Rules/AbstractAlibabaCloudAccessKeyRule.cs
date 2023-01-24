/*
 * Copyright (C) 2021-2023 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */

// Ported from ...\sonar-secrets-plugin\src\main\java\com\sonarsource\secrets\rules\AbstractAlibabaCloudAccessKeyRule.java

using SonarLint.Secrets.DotNet.Rules.Matching;

namespace SonarLint.Secrets.DotNet.Rules
{
    internal class AbstractAlibabaCloudAccessKeyRule : AbstractSecretRule
    {
        protected AbstractAlibabaCloudAccessKeyRule(string message, params ISecretsMatcher[] matchers)

              : base("S6336", "Alibaba Cloud AccessKeys should not be disclosed", message, matchers)
        { }
    }
}
