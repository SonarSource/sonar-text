/*
 * Copyright (C) 2021 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
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
            RuleKey = ruleKey;
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
