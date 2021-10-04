﻿/*
 * Copyright (C) 2018-2021 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */

using System.Collections.Generic;

namespace SonarLint.Secrets.DotNet
{
    public interface ISecretDetector
    {
        /// <summary>
        /// Returns a list of secrets found in the input, or an empty list if no secrets are found
        /// </summary>
        IEnumerable<ISecret> Find(string input);
    }
}
