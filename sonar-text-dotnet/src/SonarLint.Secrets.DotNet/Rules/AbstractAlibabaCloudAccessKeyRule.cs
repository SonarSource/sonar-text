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
