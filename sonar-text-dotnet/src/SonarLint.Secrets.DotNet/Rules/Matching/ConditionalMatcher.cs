/*
 * Copyright (C) 2021-2023 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */

// Ported from ...\sonar-secrets-plugin\src\main\java\com\sonarsource\secrets\rules\matching\ConditionalMatcher.java

using System;
using System.Collections.Generic;

namespace SonarLint.Secrets.DotNet.Rules.Matching
{
    internal class ConditionalMatcher : ISecretsMatcher
    {
        private static readonly List<Match> EmptyList = new List<Match>();

        private readonly Predicate<string> predicate;
        private readonly ISecretsMatcher ifTrue;

        public ConditionalMatcher(Predicate<string> predicate, ISecretsMatcher ifTrue)
        {
            this.predicate = predicate;
            this.ifTrue = ifTrue;
        }

        public List<Match> FindIn(string input)
        {
            return predicate(input) ? ifTrue.FindIn(input) : EmptyList;
        }
    }
}
