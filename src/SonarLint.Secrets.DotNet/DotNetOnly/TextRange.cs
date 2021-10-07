/*
 * Copyright (C) 2018-2021 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */

// The Java code uses a TextRange class defined in the Sonar API (org.sonar.api.batch.fs.TextRange).

using System.Diagnostics;

namespace SonarLint.Secrets.DotNet.DotNetOnly
{
    internal class TextRange
    {
        public TextRange(int start, int end)
        {
            Debug.Assert(start >= 0, "Start index should be greater than zero");
            Debug.Assert(end >= start, "End index should be equal to or after the end index");

            StartIndex = start;
            EndIndex = end;
        }

        public int StartIndex { get; }
        public int EndIndex { get; }

        public bool overlap(TextRange other) =>
            IndexIsInRange(StartIndex, other) ||
            IndexIsInRange(EndIndex, other) ||
            RangeIsCompletelyContained(other, this);

        private static bool IndexIsInRange(int index, TextRange other) =>
            index >= other.StartIndex && index <= other.EndIndex;

        private static bool RangeIsCompletelyContained(TextRange candidateRange, TextRange possibleContainer) =>
            (possibleContainer.StartIndex < candidateRange.StartIndex && possibleContainer.EndIndex > candidateRange.EndIndex);
    }
}
