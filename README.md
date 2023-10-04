# SonarText: detect BIDI Characters Vulnerabilities and Leaking Secrets
[![Build Status](https://api.cirrus-ci.com/github/SonarSource/sonar-text.svg?branch=master)](https://cirrus-ci.com/github/SonarSource/sonar-text)
[![Quality Gate Status](https://next.sonarqube.com/sonarqube/api/project_badges/measure?project=org.sonarsource.text%3Atext&metric=alert_status&token=75147023237a0ed7ea1a5efc5fe7ce286061ad6f)](https://next.sonarqube.com/sonarqube/dashboard?id=org.sonarsource.text%3Atext)
[![Coverage](https://next.sonarqube.com/sonarqube/api/project_badges/measure?project=org.sonarsource.text%3Atext&metric=coverage&token=75147023237a0ed7ea1a5efc5fe7ce286061ad6f)](https://next.sonarqube.com/sonarqube/dashboard?id=org.sonarsource.text%3Atext)

This SonarSource project is a static code analyzer made to detect:
* BIDI Characters Vulnerabilities
* Leaking Secrets/Tokens

It is a component of the Sonar Solution. It is embedded in SonarLint, SonarQube, and SonarCloud.
This component helps you prevent the leakage of secrets even before you push them into your repository thanks to SonarLint.

# Features
* 110+ secret patterns supported and detected by [60+](https://rules.sonarsource.com/secrets/) rules
* detection of [BIDI characters](https://rules.sonarsource.com/text/) that could lead to attacks
* detection of secrets in all files indexed by Sonar products

### Build

*Prerequisite*

- Java 11

```shell
./gradlew build
```

Apply code formatting

```shell
./gradlew spotlessApply
```

### Plugin Integration tests

```shell
./gradlew :its:plugin:test -Penable_its=true -Dsonar.runtimeVersion=LATEST_RELEASE
```

### Rules Integration tests

```shell
./gradlew :its:ruling:test -Penable_its=true -Dsonar.runtimeVersion=LATEST_RELEASE
```

### Update rule description

Update all rule descriptions.

```shell
./gradlew ruleApiUpdate
```

There are also tasks: `ruleApiUpdateSecrets` and `ruleApiUpdateText` for updating Secrets and Text rule descriptions.

### Generate new rule description

To fetch static files for a rule SXXXX from RSPEC, execute the one of following command:
```shell
./gradlew ruleApiUpdateRuleSecrets -Prule=SXXXX
./gradlew ruleApiUpdateRuleText -Prule=SXXXX
```

### Generate files to include new secrets

After the change, addition or removal of secret specifications, this script can be run to generate the Java classes that are needed
for the inclusion or deletion of these secrets and to update static RSPEC files.

As we use the enforcer plugin to define a file size of the build, this can lead to test failures after adding new secret specifications.
The `<minsize>` and `<maxsize>` can be changed in `sonar-text-plugin/pom.xml`.
```shell
./secretSpecificationInclusionGenerator.sh
```

### Verify Regexes

The Regular Expressions provided in secrets specification should be verified to avoid catastrophic backtracking and other issues.
Currently, the Sonar products doesn't scan YAML files for Regex problems.
To avoid potential problems the [SecretsRegexTest](sonar-text-plugin/src/test/java/org/sonar/plugins/secrets/utils/SecretsRegexTest.java) was prepared for validating regexes.
There is a simple way to validate all specification files and a single one. 
Currently, tests are disabled, as the issues need to be reviewed first.

There is also a way of running this check from command line.

```shell
./gradlew --rerun-tasks :sonar-text-plugin:test --console plain --tests SecretsRegexTest.shouldValidateSingleFile -Dfilename=google-oauth2.yaml
```

### License

Copyright 2012-2023 SonarSource.

Licensed under the [GNU Lesser General Public License, Version 3.0](https://www.gnu.org/licenses/lgpl.txt)
