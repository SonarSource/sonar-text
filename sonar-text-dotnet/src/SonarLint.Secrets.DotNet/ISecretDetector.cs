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
