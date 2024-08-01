# SonarText: detect BIDI Characters Vulnerabilities and Leaking Secrets

[![Build Status](https://api.cirrus-ci.com/github/SonarSource/sonar-text.svg?branch=master)](https://cirrus-ci.com/github/SonarSource/sonar-text)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=org.sonarsource.text%3Atext&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=org.sonarsource.text%3Atext)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=org.sonarsource.text%3Atext&metric=coverage)](https://sonarcloud.io/summary/new_code?id=org.sonarsource.text%3Atext)

This SonarSource project is a static code analyzer made to detect:

* BIDI Characters Vulnerabilities
* Leaking Secrets/Tokens

It is a component of the
Sonar [Clean Code solution](https://www.sonarsource.com/solutions/clean-code/?utm_medium=referral&utm_source=github&utm_campaign=clean-code&utm_content=sonar-text).
It is embedded in SonarLint, SonarQube, and SonarCloud.
This component helps you prevent the leakage of secrets even before you push them into your repository thanks to SonarLint.

# Features

* 110+ secret patterns supported and detected by [60+](https://rules.sonarsource.com/secrets/) rules
* detection of [BIDI characters](https://rules.sonarsource.com/text/) that could lead to attacks
* detection of secrets in all files indexed by Sonar products

## Build

*Prerequisite*

- Java 17

Simple build skipping integration tests.

```shell
./gradlew build
```

### Apply code formatting.

```shell
./gradlew spotlessApply
```

### Update rule description

Update all rule descriptions.

```shell
./gradlew ruleApiUpdate
```

There are also tasks: `ruleApiUpdateSecrets`, `ruleApiUpdateText` and `ruleApiUpdateEnterpriseSecrets`, `ruleApiUpdateEnterpriseText` 
for updating Secrets, Text and Enterprise Text and Secrets rule descriptions.

### Generate new rule description

To fetch static files for a rule SXXXX from RSPEC, execute the one of following commands:

```shell
./gradlew ruleApiGenerateRuleSecrets -Prule=SXXXX
./gradlew ruleApiGenerateRuleText -Prule=SXXXX
./gradlew ruleApiGenerateRuleEnterpriseSecrets -Prule=SXXXX
./gradlew ruleApiGenerateRuleEnterpriseText -Prule=SXXXX
```

### Generate files to include new secrets

After the change, addition or removal of secret specifications, this script can be run to generate the Java classes that are needed
for the inclusion or deletion of these secrets and to update static RSPEC files.

As we use the enforcer plugin to define the file size of the build, this can lead to test failures after adding new secret specifications.
The `<minsize>` and `<maxsize>` can be changed in `sonar-text-plugin/build.gradle.kts` (search for `enforceJarSize`).

If the static RSPEC files are currently not merged on master, it is possible to specify a branch to fetch the files from.
This can be done by adding the name of the branch as an argument to the script, like shown below.
Use this feature with caution, due to a limitation of the underlying library in the rule api, not all branch names are supported, and it's
not clear which ones do work.

```shell
./secretSpecificationInclusionGenerator.sh
./secretSpecificationInclusionGenerator.sh branchName
```

### Verify Regexes

The Regular Expressions provided in the secrets specification should be verified to avoid catastrophic backtracking and other issues.
Currently, the Sonar products don't scan YAML files for Regex problems.
To avoid potential problems the [SecretsRegexTest](sonar-text-plugin/src/test/java/org/sonar/plugins/secrets/utils/SecretsRegexTest.java)
was prepared for validating regexes.
There is a simple way to validate all specification files and a single one.
Currently, tests are disabled, as the issues need to be reviewed first.

There is also a way of running this check from the command line.

```shell
./gradlew --rerun-tasks :sonar-text-plugin:test --console plain --tests SecretsRegexTest.shouldValidateSingleFile -Dfilename=google-oauth2.yaml
```

### License

Copyright 2012-2024 SonarSource.

Licensed under the [GNU Lesser General Public License, Version 3.0](https://www.gnu.org/licenses/lgpl.txt)
