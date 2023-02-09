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
            this.pattern = new Regex(stringPattern, RegexOptions.Compiled, RegexConstants.DefaultTimeout);
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
