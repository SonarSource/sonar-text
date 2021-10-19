/*
 * Copyright (C) 2021 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */

using System.Collections.Generic;

namespace SonarLint.Secrets.DotNet
{
    public interface ISecretDetector
    {
        /// <summary>
        /// The unique identifier for the rule
        /// </summary>
        string RuleKey { get; }

        /// <summary>
        /// The issue message to display
        /// </summary>
        string Message { get; }

        /// <summary>
        /// Returns a list of secrets found in the input, or an empty list if no secrets are found
        /// </summary>
        IEnumerable<ISecret> Find(string input);
    }
}
