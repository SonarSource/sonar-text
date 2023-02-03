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

// Ported from ...\sonar-secrets-plugin\src\main\java\com\sonarsource\secrets\rules\AwsAccessKeyRule.java

using SonarLint.Secrets.DotNet.Rules.Matching;
using System.ComponentModel.Composition;

namespace SonarLint.Secrets.DotNet.Rules
{
    [Export(typeof(ISecretDetector))]
    internal class AwsAccessKeyRule : AbstractAwsRule
    {
        public AwsAccessKeyRule()

              : base("Make sure this AWS Secret Access Key is not disclosed.",
                new RegexMatcher("(?i)aws.{0,50}['\"`]([0-9a-z\\/+]{40})['\"`]"),
                new RegexMatcher("(?i)\\b(?:AWS)?_?SECRET_?(?:ACCESS)?_?KEY\\b.{0,10}([0-9a-z\\/+]{40})"))
        {
        }

        protected override bool IsProbablyFalsePositive(string matchedText) =>
            EntropyChecker.HasLowEntropy(matchedText);
    }
}
