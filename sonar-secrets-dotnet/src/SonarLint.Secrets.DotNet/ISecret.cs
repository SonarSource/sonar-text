/*
 * Copyright (C) 2021-2022 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */

using System;

namespace SonarLint.Secrets.DotNet
{
    public interface ISecret
    {
        /// <summary>
        /// 0-based start position for the detected secret
        /// </summary>
        int StartIndex { get; }

        /// <summary>
        /// Number of characters in the secret
        /// </summary>
        int Length { get; }
    }

    internal class Secret : ISecret
    {
        public Secret(int startIndex, int length)
        {
            if (startIndex < 0)
            {
                throw new ArgumentOutOfRangeException(nameof(startIndex));
            }

            if (length < 1)
            {
                throw new ArgumentOutOfRangeException(nameof(length));
            }

            StartIndex = startIndex;
            Length = length;
        }

        public int StartIndex { get; }
        public int Length { get; }
    }
}
