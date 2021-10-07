/*
 * Copyright (C) 2018-2021 SonarSource SA
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
    }
}
