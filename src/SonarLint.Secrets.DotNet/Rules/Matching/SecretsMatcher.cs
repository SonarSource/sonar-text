/*
 * Copyright (C) 2021 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */

using System;
using System.Collections.Generic;

namespace SonarLint.Secrets.DotNet.Rules.Matching
{
    internal interface SecretsMatcher {
        List<Match> findIn(String content);
    }
}
