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

// NOTE: the Match API in .NET is not the same as the Java version

namespace SonarLint.Secrets.DotNet.Rules.Matching
{
    internal class Match
    {
        public Match(string text, int startIndex, int length)
        {
            Text = text;
            StartIndex = startIndex;
            Length = length;
        }

        public string Text { get; }
        public int StartIndex{ get; }
        public int Length { get; }

        public bool Overlaps(Match other)
        {
            int thisEndIndex = StartIndex + Length - 1;
            int otherEndIndex = other.StartIndex + other.Length - 1;

            return
                // 1. Our start index is inside the other Match
                (StartIndex >= other.StartIndex && StartIndex <= otherEndIndex) ||

                // 2. Our end index is inside the other Match
                (thisEndIndex >= other.StartIndex && thisEndIndex <= otherEndIndex) ||

                // 3. We completely contain the other match
                (this.StartIndex < other.StartIndex && thisEndIndex > otherEndIndex);
        }
    }
}
