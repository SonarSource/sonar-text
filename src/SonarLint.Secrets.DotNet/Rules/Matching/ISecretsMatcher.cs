/*
 * Copyright (C) 2021-2022 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */

using System.Collections.Generic;

namespace SonarLint.Secrets.DotNet.Rules.Matching
{
    internal interface ISecretsMatcher
    {
        List<Match> FindIn(string input);
    }
}
