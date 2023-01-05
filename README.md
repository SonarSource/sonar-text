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

### License

Copyright 2012-2021 SonarSource.

Licensed under the [GNU Lesser General Public License, Version 3.0](https://www.gnu.org/licenses/lgpl.txt)
