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

// NOTE: logically the processing should be the same as the Java AbstractSecretRule (src\main\java\com\sonarsource\secrets\rules\AbstractSecretRule.java)

using SonarLint.Secrets.DotNet.Rules.Matching;
using System.Collections.Generic;
using System.Linq;

namespace SonarLint.Secrets.DotNet.Rules
{
    internal class AbstractSecretRule : ISecretDetector
    {
        private readonly List<ISecretsMatcher> matchers;

        protected AbstractSecretRule(string ruleKey, string name, string message, params ISecretsMatcher[] matchers)
        {
            // Rule keys are in the form "[repo id]:[rule id]".
            // The repo id for all SonarLint secrets rules has to be "secrets"
            // to match what is used on the rules website e.g.
            //   https://rules.sonarsource.com/secrets/RSPEC-6290
            RuleKey = "secrets:" + ruleKey;

            Name = name;
            Message = message;
            this.matchers = matchers.ToList();
        }

        public string RuleKey { get; }

        public string Name { get; }

        public string Message { get; }

        protected virtual bool IsProbablyFalsePositive(string matchedText) { return false; }

        public IEnumerable<ISecret> Find(string input)
        {
            var nonOverlappingTruePositives = new List<Match>();

            var allTruePositiveMatches = matchers
                .SelectMany(matcher => matcher.FindIn(input))
                .Where(match => !IsProbablyFalsePositive(match.Text));

            // Discard any overlapping matches
            foreach (var match in allTruePositiveMatches)
            {
                if (IsNewMatchNonOverlapping(nonOverlappingTruePositives, match))
                {
                    nonOverlappingTruePositives.Add(match);
                }
            }

            return nonOverlappingTruePositives
                .Select(match => new Secret(match.StartIndex, match.Length))
                .ToArray();
        }

        private static bool IsNewMatchNonOverlapping(List<Match> currentMatches, Match newMatch)
        {
            return !currentMatches.Any(x => x.Overlaps(newMatch));
        }
    }
}
