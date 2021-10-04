/*
 * Copyright (C) 2018-2021 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */

using System.ComponentModel.Composition;
using FluentAssertions;

namespace SonarLint.VisualStudio.Integration.UnitTests
{
    /// <summary>
    /// Generic class that MEF imports an arbitrary type.
    /// Used when testing that platforms extensions can be imported as expected.
    /// </summary>
    public class SingleObjectImporter<T> where T : class
    {
        [Import]
        public T Import { get; set; }

        #region Assertions

        public void AssertImportIsNotNull()
        {
            this.Import.Should().NotBeNull();
        }

        public void AssertImportIsInstanceOf<TExpected>()
        {
            this.AssertImportIsNotNull();
            this.Import.Should().BeAssignableTo<TExpected>();
        }

        #endregion Assertions
    }
}