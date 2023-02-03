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

using FluentAssertions;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using SonarLint.Secrets.DotNet.Rules;
using SonarLint.VisualStudio.Integration.UnitTests;
using System.Linq;
using static SonarLint.Secrets.DotNet.UnitTests.TestUtils;

namespace SonarLint.Secrets.DotNet.UnitTests.Rules
{
    [TestClass]
    public class AzureStorageAccountKeyRuleTest
    {
        [TestMethod]
        public void MefCtor_CheckIsExported()
        {
            MefTestHelpers.CheckTypeCanBeImported<AzureStorageAccountKeyRule, ISecretDetector>(null, null);
        }

        [TestMethod]
        public void TestRuleProperties()
        {
            var testSubject = new AzureStorageAccountKeyRule();

            testSubject.RuleKey.Should().Be("secrets:S6338");
            testSubject.Name.Should().Be("Azure Storage Account Keys should not be disclosed");
            testSubject.Message.Should().Be("Make sure this Azure Storage Account Key is not disclosed.");
        }

        [TestMethod]
        public void TestRuleFirstRegexPositive()
        {
            var testSubject = new AzureStorageAccountKeyRule();

            var input = "(async function main() {\n" +
              "  const account = process.env.ACCOUNT_NAME || \"accountname\";\n" +
              "  const accountKey = process.env.ACCOUNT_KEY || \"4dVw+l0W8My+FwuZ08dWXn+gHxcmBtS7esLAQSrm6/Om3jeyUKKGMkfAh38kWZlItThQYsg31v23A0w/uVP4pg==\";\n" +
              "  const sharedKeyCredential = new StorageSharedKeyCredential(account, accountKey);\n" +
              "  const blobServiceClient = new BlobServiceClient(\n" +
              "    `https://${account}.blob.core.windows.net`,\n" +
              "    sharedKeyCredential\n" +
              "  );\n" +
              "}";

            var secrets = testSubject.Find(input);

            secrets.Count().Should().Be(1);
            CheckExpectedSecretFound(input, "4dVw+l0W8My+FwuZ08dWXn+gHxcmBtS7esLAQSrm6/Om3jeyUKKGMkfAh38kWZlItThQYsg31v23A0w/uVP4pg==", secrets.First());
            CrossCheckWithJavaResult(input, new(3, 49, 3, 137), secrets.First());
        }

        [TestMethod]
        public void TestRuleSecondRegexPositive()
        {
            var testSubject = new AzureStorageAccountKeyRule();

            var input = "const connStr = \"DefaultEndpointsProtocol=https;AccountName=testaccountname;AccountKey=4dVw+l0W8My+FwuZ08dWXn+gHxcmBtS7esLAQSrm6/Om3jeyUKKGMkfAh38kWZlItThQYsg31v23A0w/uVP4pg==\";";

            var secrets = testSubject.Find(input);

            secrets.Count().Should().Be(1);
            CheckExpectedSecretFound(input, "4dVw+l0W8My+FwuZ08dWXn+gHxcmBtS7esLAQSrm6/Om3jeyUKKGMkfAh38kWZlItThQYsg31v23A0w/uVP4pg==", secrets.First());
            CrossCheckWithJavaResult(input, new(1, 87, 1, 175), secrets.First());
        }

        [TestMethod]
        public void TestRuleSecondRegexPositiveEvenWhenCoreWindowsNetStringPresent()
        {
            var testSubject = new AzureStorageAccountKeyRule();

            var input = "const connStr = \"DefaultEndpointsProtocol=https;AccountName=testaccountname;AccountKey=4dVw+l0W8My+FwuZ08dWXn+gHxcmBtS7esLAQSrm6/Om3jeyUKKGMkfAh38kWZlItThQYsg31v23A0w/uVP4pg==;EndpointSuffix=core.windows.net\";";

            var secrets = testSubject.Find(input);

            secrets.Count().Should().Be(1);
            CheckExpectedSecretFound(input, "4dVw+l0W8My+FwuZ08dWXn+gHxcmBtS7esLAQSrm6/Om3jeyUKKGMkfAh38kWZlItThQYsg31v23A0w/uVP4pg==", secrets.First());
            CrossCheckWithJavaResult(input, new(1, 87, 1, 175), secrets.First());
        }

        [TestMethod]
        public void TestRuleRegexNegative()
        {
            var testSubject = new AzureStorageAccountKeyRule();

            var secrets = testSubject.Find("AccountKey=BtS7esLAQSrm6/Om3jeyUKKGMkfAh38kWZlItThQYsg31v23A0w/uVP4pg==\";");

            secrets.Should().BeEmpty();
        }
    }
}
