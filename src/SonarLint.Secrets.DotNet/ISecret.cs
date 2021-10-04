/*
 * Copyright (C) 2018-2021 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */

using System;

namespace SonarLint.Secrets.DotNet
{
    public interface ISecret
    {
        string RuleKey { get; }

        /// <summary>
        /// 0-based start position for the detected secret
        /// </summary>
        int StartIndex { get; }

        /// <summary>
        /// 0-based end position for the detected secret
        /// </summary>
        int EndIndex { get; }
    }

    internal class Secret : ISecret
    {
        public Secret(string ruleKey, int startIndex, int endIndex)
        {
            RuleKey = ruleKey ?? throw new ArgumentNullException(nameof(ruleKey));

            if (startIndex < 0)
            {
                throw new ArgumentOutOfRangeException(nameof(startIndex));
            }

            if (endIndex < 0)
            {
                throw new ArgumentOutOfRangeException(nameof(endIndex));
            }

            StartIndex = startIndex;
            EndIndex = endIndex;
        }

        public string RuleKey { get; }
        public int StartIndex { get; }
        public int EndIndex { get; }
    }
}
