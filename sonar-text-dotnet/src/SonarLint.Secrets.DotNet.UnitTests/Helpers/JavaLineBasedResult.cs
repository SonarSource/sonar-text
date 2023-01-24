/*
 * Copyright (C) 2021-2023 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */

namespace SonarLint.Secrets.DotNet.UnitTests
{
    // Data-class matching the Java representation of a detected secret
    public class JavaLineBasedResult
    {
        public JavaLineBasedResult(int startLine, int startLineOffset, int endLine, int endLineOffset)
        {
            StartLine = startLine;
            StartLineOffset = startLineOffset;
            EndLine = endLine;
            EndLineOffset = endLineOffset;
        }

        public int StartLine { get; }
        public int StartLineOffset { get; }
        public int EndLine { get; }
        public int EndLineOffset { get; }
    }
}
