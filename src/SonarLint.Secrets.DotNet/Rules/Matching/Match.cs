/*
 * Copyright (C) 2021 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */

// NOTE: the Match API in .NET is not the same as the Java version

namespace SonarLint.Secrets.DotNet.Rules.Matching
{
    internal class Match
    {
        public Match(string text, int startIndex, int length) {
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
