/*
 * SonarAnalyzer for Text
 * Copyright (C) 2021-2023 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
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
