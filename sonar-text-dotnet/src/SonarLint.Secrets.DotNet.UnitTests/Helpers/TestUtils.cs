/*
 * Copyright (C) 2021-2023 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */

using FluentAssertions;

namespace SonarLint.Secrets.DotNet.UnitTests
{
    internal static class TestUtils
    {
        public static void CheckExpectedSecretFound(string input, string expectedText, ISecret actual)
        {
            var expectedStartIndex = input.IndexOf(expectedText, 0);

            actual.StartIndex.Should().Be(expectedStartIndex);
            actual.Length.Should().Be(expectedText.Length);
        }

        public static void CrossCheckWithJavaResult(string input, JavaLineBasedResult javaResult, ISecret dotNetResult)
        {
            javaResult.StartLine.Should().Be(javaResult.EndLine, "Invalid assumption in test code: not expecting the Java result to be multi-line");

            // Calculate the .NET equivalents of the Java line-based result
            var javaAbsoluteStatIndex = CountCharsInLines(input, javaResult.StartLine - 1) + javaResult.StartLineOffset;
            var javaLength = javaResult.EndLineOffset - javaResult.StartLineOffset;

            dotNetResult.StartIndex.Should().Be(javaAbsoluteStatIndex);
            dotNetResult.Length.Should().Be(javaLength);
        }

        private static int CountCharsInLines(string input, int linesToCount)
        {
            var charCount = 0;
            var countedLines = 0;
            var currentIndex = 0;
            while (countedLines < linesToCount)
            {
                charCount++;
                if (input[currentIndex] == '\n')
                {
                    countedLines++;
                }

                currentIndex++;
            }
            return charCount;
        }
    }
}
