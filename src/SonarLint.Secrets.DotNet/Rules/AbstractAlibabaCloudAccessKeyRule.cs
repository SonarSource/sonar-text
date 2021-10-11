/*
 * Copyright (C) 2018-2021 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */

// Ported from ...\sonar-secrets-plugin\src\main\java\com\sonarsource\secrets\rules\AbstractAlibabaCloudAccessKeyRule.java

using SonarLint.Secrets.DotNet.Rules.Matching;

namespace SonarLint.Secrets.DotNet.Rules
{
    internal abstract class AbstractAlibabaCloudAccessKeyRule : AbstractSecretRule
    {
        protected AbstractAlibabaCloudAccessKeyRule(string message, params SecretsMatcher[] matchers)

              : base("S6336", "Alibaba Cloud AccessKeys should not be disclosed", message, matchers)
        { }
    }
}