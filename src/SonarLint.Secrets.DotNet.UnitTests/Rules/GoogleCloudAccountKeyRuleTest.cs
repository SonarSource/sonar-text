/*
 * Copyright (C) 2021-2022 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */

// Ported from ...\sonar-secrets-plugin\src\test\java\com\sonarsource\secrets\rules\GoogleCloudAccountKeyRuleTest.java

using FluentAssertions;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using SonarLint.Secrets.DotNet.Rules;
using SonarLint.VisualStudio.Integration.UnitTests;
using System.Linq;
using static SonarLint.Secrets.DotNet.UnitTests.JavaTestFileUtils;
using static SonarLint.Secrets.DotNet.UnitTests.TestUtils;

namespace SonarLint.Secrets.DotNet.UnitTests.Rules
{
    [TestClass]
    public class GoogleCloudAccountKeyRuleTest
    {
        [TestMethod]
        public void MefCtor_CheckIsExported()
        {
            MefTestHelpers.CheckTypeCanBeImported<GoogleCloudAccountKeyRule, ISecretDetector>(null, null);
        }

        [TestMethod]
        public void TestRuleProperties()
        {
            var testSubject = new GoogleCloudAccountKeyRule();

            testSubject.RuleKey.Should().Be("S6335");
            testSubject.Name.Should().Be("Google Cloud service accounts keys should not be disclosed");
            testSubject.Message.Should().Be("Make sure this Google Cloud service account key is not disclosed.");
        }

        [TestMethod]
        public void TestRuleRegexPositive()
        {
            var testSubject = new GoogleCloudAccountKeyRule();

            // Note: the expected value in the file contains backslashes, which need to be escaped in the following string variable
            var expectedSecret = "-----BEGIN PRIVATE KEY-----\\nMIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCqypYh1ms+SNYj\\n8o7F1a4v9JO2h4Rj6TO7sUaiBlmqERTnjMdiNmQiJB6zS+iVExh804vleIYHv75E\\n5BEZxeUTuA6s3PDs9fGhql6YFY8p7sVarbfg3X50trVvfz85XmvnNj/+dlWTu73g\\nAnsCiDmgdjmQXbZsAXaY0S3eZO2cgkG7btyG31+uqSOUrohscJkLIqEaI4p6rDWa\\nw9TgtFiGt8GlrRSAEXREmbHQeOvgfPdSHVCp0MuZ2XQPLH+zoydk5AxgMDEPPWOn\\ngMEmnPU41S9VT4CYcTNs4L+keoECNggRqYvNfGr6MdQmfOkwk6VU31DLKLNtzkKS\\n+9ucI9wVAgMBAAECggEAGa8If8MucnnilhpSZSbaoISPuBnOC3ZU3Mtue0woLL6P\\n3bhafYb/bPDwmvb13YE3JAJuHc6PnVe05chEWBNd/SanMlCxfHkJ4s1rl0RauKwb\\nCqVoKw00CJo3U7KXj+mGoYkTe5FREjvf9HF7DSjuPZa51l95YOmha569Xp3f67Kd\\n31olO+gqKvZJhgvDMjhSMormGRtucCwKigTvqT+jvw0NbajtyReD5AnslHroRI6h\\nawMASrCl2gp6iR5h98FP9fi5dtSnTyWQjoWZqHYov95h490t3ZDUAvfjhNIEBiaC\\n5PgG8NqiKRu7gGjZw66/vYox5V/tscCOtZ4M2/LCawKBgQDhF9ESmsCh3lMIwVpn\\nJA4xrKpA89Bxk7X11L5ORCXeI+hicwQpv3aaUDFhSw039joSyBUsu6+cnR/2rqor\\nhHJ4aRfNByFjWallp/dFoEjTAVLLX3YYWGjeFcnls8yIjC273TuztrC45+BTwPQ/\\npi49cHVib1upZGogKd765cFovwKBgQDCPga9afKA5fmYDpV9nNEZkDyJqHx84OxV\\ng2iQrBT91EnfQ4jWwDUbPgoV/PxNFGIJriO+h80jpOFfdr8BR8tmagGkNW35PHVW\\nA23kXA77WBDlwLOvYJMayLiS+6qPD9GKXUOs68330nKhV7gCw9ptP6dj8nm4tARC\\nqtzWEAW8KwKBgQDc3zX0f43OSA5KZRCEbMjQzZEyswwyprLCSsOCoTRXSfzZveqz\\n3IBQ46fQxIhwBVju3Q6KGpEzqKqYsMOieBCrPtRwBzMn/e9PJQZqd+F9y6qmjUGg\\nmgAtDExU7Z0h0AuAcJIIwpeemUlyZcBGRJbTCurkcEkNas8ISI3YvGKQmQKBgGuT\\nfpgYvT22IG2eZhyTZRsLTvvOXGOtDjat+JNnOpj5oZANDxQgj0jvKxbSbckiqMlD\\nsHgtLee3wTnlwhMrd/LYcuBG1wlZ+oIQlCQM+B8rvu//sYRHsDD9SXvd9bAVyq8e\\nyARU9FV0MBg7RKYDYk1vG323JMKusE1b48KKTSLRAoGAGGILCOCFKlBt43u5bj9j\\n6xDEOBn/LG8zyuGOFqIkE3iHbLuMaFpXpKT4dsx51gSrN67Zsgmj/Zf2OxU7vTfF\\nHP+SrqniXDOSsmjHWXs+V7enSC/7e/Lwaw8rNI42hizLMzNNqPx+5Vd/vBIN6z0U\\ncU6a4WWC772nb+mLIODMlx0=\\n-----END PRIVATE KEY-----\\n";

            var input = readFileAndNormalize("src/test/files/google-cloud-account-key/GoogleCloudAccountPositive.json", UTF_8);
            var secrets = testSubject.Find(input);

            secrets.Count().Should().Be(1);
            CheckExpectedSecretFound(input, expectedSecret, secrets.First());
            CrossCheckWithJavaResult(input, new(5, 18, 5, 1750), secrets.First());
        }

        [TestMethod]
        public void TestRuleRegexNegative()
        {
            var testSubject = new GoogleCloudAccountKeyRule();

            var secrets = testSubject.Find(readFileAndNormalize("src/test/files/google-cloud-account-key/GoogleCloudAccountNegative.json", UTF_8));

            secrets.Should().BeEmpty();
        }
    }
}