﻿/*
 * Copyright (C) 2021-2022 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */

using System.Collections.Generic;
using System.Text.RegularExpressions;
using MatchResult = System.Text.RegularExpressions.Match;

namespace SonarLint.Secrets.DotNet.Rules.Matching
{
    internal class RegexMatcher : ISecretsMatcher
    {
        private readonly Regex pattern;

        public RegexMatcher(string stringPattern)
        {
            this.pattern = new Regex(stringPattern, RegexOptions.Compiled);
        }

        public List<Match> FindIn(string input)
        {
            List<Match> matches = new List<Match>();

            foreach (MatchResult matchResult in pattern.Matches(input))
            {
                // Note: hard-coded assumption that the text to highlight is in the second group (index = 1)
                var group = matchResult.Groups[1];
                matches.Add(new Match(group.Value, group.Index, group.Length));
            }

            return matches;
        }
    }
}
