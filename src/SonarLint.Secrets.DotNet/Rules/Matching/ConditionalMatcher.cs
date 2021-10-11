/*
 * Copyright (C) 2018-2021 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */

// Ported from ...\sonar-secrets-plugin\src\main\java\com\sonarsource\secrets\rules\matching\ConditionalMatcher.java

using System;
using System.Collections.Generic;

namespace SonarLint.Secrets.DotNet.Rules.Matching
{
    internal class ConditionalMatcher : SecretsMatcher
    {
        private static readonly List<Match> EmptyList = new List<Match>();

        private readonly Predicate<string> predicate;
        private readonly SecretsMatcher ifTrue;

        public ConditionalMatcher(Predicate<string> predicate, SecretsMatcher ifTrue)
        {
            this.predicate = predicate;
            this.ifTrue = ifTrue;
        }

        public List<Match> findIn(string content)
        {
            return predicate(content) ? ifTrue.findIn(content) : EmptyList;
        }
    }
}
