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
