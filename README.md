# SonarSource Text Plugin
[![Build Status](https://api.cirrus-ci.com/github/SonarSource/sonar-text.svg?branch=master)](https://cirrus-ci.com/github/SonarSource/sonar-text)
[![Quality Gate Status](https://next.sonarqube.com/sonarqube/api/project_badges/measure?project=org.sonarsource.text%3Atext&metric=alert_status&token=75147023237a0ed7ea1a5efc5fe7ce286061ad6f)](https://next.sonarqube.com/sonarqube/dashboard?id=org.sonarsource.text%3Atext)
[![Coverage](https://next.sonarqube.com/sonarqube/api/project_badges/measure?project=org.sonarsource.text%3Atext&metric=coverage&token=75147023237a0ed7ea1a5efc5fe7ce286061ad6f)](https://next.sonarqube.com/sonarqube/dashboard?id=org.sonarsource.text%3Atext)

Plugin used to analyze all files containing text.

### Build

*Prerequisite*

- Java 11
- maven

```shell
mvn clean install
```

### Plugin Integration tests

```shell
cd its/ruling
mvn verify -Dsonar.runtimeVersion=LATEST_RELEASE -B -e -V
```

### Rules Integration tests

```shell
cd its/plugin
mvn verify -Dsonar.runtimeVersion=LATEST_RELEASE -B -e -V
```

### Check if dependencies need to be updated

If you have a `~/.m2/settings.xml` containing some private servers and repositories, it's safer to use
a custom emtpy `settings.xml` to only look for latest version publicly available on maven central.

```shell
echo "<settings/>" > empty-settings.xml
mvn -s empty-settings.xml versions:display-dependency-updates
rm empty-settings.xml
```

### Update rule description

```shell
cd sonarpedia-secrets
java -jar ../../sonar-rule-api/target/rule-api-2.4.0-SNAPSHOT.jar update
cd ../sonarpedia-text
java -jar ../../sonar-rule-api/target/rule-api-2.4.0-SNAPSHOT.jar update
```

### Create new secrets

See [CONTRIBUTING.md](./CONTRIBUTING.md).

### Generate files to include new secrets

After the change, addition or removal of secret specifications, this script can be run to generate the Java classes that are needed 
for the inclusion or deletion of these secrets.

To run this script you should specify the file name of the rule-api jar `<ruleApiFileName>`, otherwise a default value of `rule-api-snap.jar` will be assumed.
The rule-api jar needs to be located in the root folder of the project.

As we use the enforcer plugin to define a file size of the build, this can lead to test failures after adding new secret specifications.
The `<minsize>` and `<maxsize>` can be changed in `sonar-text-plugin/pom.xml`.
```shell
./secretSpecificationInclusionGenerator.sh <ruleApiFileName>
```

### License

Copyright 2012-2021 SonarSource.

Licensed under the [GNU Lesser General Public License, Version 3.0](https://www.gnu.org/licenses/lgpl.txt)
