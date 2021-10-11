/*
    * Copyright (C) 2018-2021 SonarSource SA
    * All rights reserved
    * mailto:info AT sonarsource DOT com
    */

// Ported from ...\sonar-secrets-plugin\src\main\java\com\sonarsource\secrets\rules\AbstractAwsRule.java

using SonarLint.Secrets.DotNet.Rules.Matching;
using System;

namespace SonarLint.Secrets.DotNet.Rules
{
    internal class AbstractAwsRule : AbstractSecretRule
    {
        private const string RULE_KEY = "S6290";

        protected AbstractAwsRule(String message, params SecretsMatcher[] matchers)
          : base(RULE_KEY, "Amazon Web Services credentials should not be disclosed", message, matchers)
        {
        }
    }
}
