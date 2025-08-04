# Project Overview

This project is a SonarQube plugin that provides a collection of checks for detecting secrets in code. It is designed to help developers
identify and manage sensitive information, such as API keys and passwords, that may inadvertently be included in their source code.
The plugin is available in different packaging depending on the SonarQube edition. Specifications for detection of secrets are defined as
YAML files. YAML files comply with the [schema](../sonar-text-plugin/src/main/resources/org/sonar/plugins/secrets/configuration/specifications/specification-json-schema.json).

# Project layout

The root directory contains a root `build.gradle.kts` that builds a collection of subprojects into SonarQube plugins.
- `sonar-text-plugin`: Contains the main plugin code, as well as packaging of the plugin for free edition of SonarQube.
- `private/`: Contains the code for the commercial editions of the plugin with additional checks, as well as integration tests.
- `private/sonar-text-developer-plugin`: Contains the code for the developer edition of the plugin, which includes additional checks.
- `private/sonar-text-enterprise-plugin`: Contains the code for the enterprise edition of the plugin, which includes custom secret patterns.
- `private/its`: Contains integration tests for the plugin.

# Technology stack

The technology stack:

- Java 17
- Gradle with Kotlin DSL
- SonarQube plugin API
- JUnit 5
- Mockito
- AssertJ

# General coding guidelines

* Unless there are ambiguous names in the same file, always import classes rather than referring to them by their fully
  qualified name.
* Prefer immutable data structures and classes.
* Prefer `var` over explicit types for local variables, unless the type is not obvious from the right-hand side of the
  assignment.
* Prefer `final` for fields.
* When `for` loops could be rewritten as streams that are still easy to read and understand, prefer using streams.
* Short streams can be in a single line, and long streams should be broken before every `.` starting after the stream() method.
* Avoid nested streams
* Write comments for public APIs, Util methods, Complex Regex
* Don't use wildcard imports, i.e., `import java.util.*;`. Always import specific classes, e.g., `import java.util.List;`.

# Unit test guidelines

* Pure unit tests should be placed in the src/test/java directory of the subproject being tested, and the test class
  should be named ClassnameTest.
* Use AssertJ for test assertions. Always prefer the `assertThat` style of assertions from AssertJ over the JUnit 5
  `assertEquals` and `assertTrue` methods.
* When creating a variable or field referring to the class being tested, name it `className` (e.g., `textCheck` for
  `TextCheck` class).
* Name test methods following the pattern `shouldShowExpectedBehaviorWhenMeetsCondition`, for example
  `shouldFindAllIssuesInValidFile`.
* Always use a static import for `assertThat` from AssertJ in test classes, rather than calling it as
  `Assertions.assertThat`.
  
