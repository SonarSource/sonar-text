/*
 * SonarAnalyzer for Text
 * Copyright (C) 2021-2023 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
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
