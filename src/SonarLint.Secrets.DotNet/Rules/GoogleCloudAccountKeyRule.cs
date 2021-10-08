/*
 * Copyright (C) 2021-2021 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */

// Ported from ...\sonar-secrets-plugin\src\main\java\com\sonarsource\secrets\rules\GoogleCloudAccountKeyRule.java

using SonarLint.Secrets.DotNet.Rules.Matching;
using System.ComponentModel.Composition;

namespace SonarLint.Secrets.DotNet.Rules
{
    [Export(typeof(ISecretDetector))]
    [PartCreationPolicy(CreationPolicy.Shared)]
    internal class GoogleCloudAccountKeyRule : AbstractSecretRule
    {
        public GoogleCloudAccountKeyRule()
            : base("S6335", "Google Cloud service accounts keys should not be disclosed",
                "Make sure this Google Cloud service account key is not disclosed.",
                new RegexMatcher("\"private_key\"\\s*:\\s*\"(-----BEGIN PRIVATE KEY-----\\\\n[a-z-A-Z0-9+/=]{64}\\\\n" +
                  "[a-z-A-Z0-9+/=\\\\]+-----END PRIVATE KEY-----(:?\\\\n)?)\"")) { }

    }
}
