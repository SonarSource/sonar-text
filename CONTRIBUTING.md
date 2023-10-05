# Contribute to the secrets of sonar-text

## Development environment

1. Set up a Java development environment, or use Docker, to be ready to perform commands such as those specified in the [README.md](README.md)
2. Set up an extension that validates YAML files, such as [YAML by RedHat](https://github.com/redhat-developer/vscode-yaml)
3. Set up this [json-schema file](https://github.com/SonarSource/sonar-text/blob/master/sonar-text-plugin/src/main/resources/org/sonar/plugins/secrets/configuration/specifications/specification-json-schema.json) for your validation tooling
4. Take a look at [the existing rules](https://github.com/SonarSource/sonar-text/tree/master/sonar-text-plugin/src/main/resources/org/sonar/plugins/secrets/configuration)
5. Familiarize yourself with [SonarSource/rspec](https://github.com/SonarSource/rspec)

## Adding a secret

1. Write the yaml file
2. Validates the yaml against the schema file
3. use the script `secretSpecificationInclusionGenerator.sh <Rule-Api jar>`
  1. It will generate the java files serving as a "glue" between the yaml file and the code base
  2. If the rspec text code is already available in the master branch of [SonarSource/rspec](https://github.com/SonarSource/rspec), it will also generate additional rspec data
4. Test the code:
  1. If rspec data is available, do `mvn clean test`
  2. Else, do `mvn test -Dtest=FakecloudCheckTest` and replace this test name by the one generated previously 

## Common errors

- Modifying code from the GitHub UI and not validating it with the schema
  - It will raise tons of exceptions, and `"DeserializationException: Could not load specification from file: slack.yaml"]`  
- Mismatch from what's expected in `rules[X].examples[X].match` and what's raised  (if `containsSecret` is set to true):
  - `java.lang.AssertionError: actual is not empty while group of values to look for is.`
- If `containsSecret` is set to false but something is still raised
  - `Expecting empty but was: [DefaultIssue[ruleKey=secrets:SXXXXX,gap=<null>,overriddenSeverity=<null>,quickFixAvailable=false,ruleDescriptionContextKey=<null>,codeVariants=<null>,...,saved=true]]`
- Creating overly greedy regex:
  - The Some projects in the validation phase might not be analyzed because of time of scan
<<<<<<< HEAD
=======
  
### Not all secrets are created equal
We discovered that secrets generally fit into one of 3 categories:

1. Those that follow a pattern and contain identifiable sequences. For example, SonarQube API tokens always start with a known prefix (sqa_, sqp_ or squ_) that’s followed by 40 hexadecimal characters.
2. Those that follow a pattern, but which can only be identified by looking at the surrounding text. For example, AWS secret keys are always 40 base64 characters long, but only the surrounding text can identify whether they’re used with AWS.
3. Those that follow no fixed pattern, and which are only identifiable based on the surrounding text. For example, a PostgreSQL database password is user-chosen and must be set into a named environment variable before calling the psql command-line client.

As you go down the list, the secrets get harder to detect without also raising false positives. Category #3 is particularly difficult because the suspected secret could be a dummy value (e.g. `[password]`) or a variable that’s replaced at runtime.

A lot of tuning is needed in order to identify and exclude values that are common false positives.  
  
## code structure for secret detection
>>>>>>> f952090a5f649bce3e55a110ecf993154cd688a1

## Code structure for secret detection

Take a look at the example of a secret detection specification file for a fake
cloud provider below. This is considered the anatomy of a "good" secret detection file.

``` yaml
provider:
  metadata:
    name: FakeCloud
    category: Cloud Provider
    message: Make sure this FakeCloud API key gets revoked, changed, and removed from the code.
  detection:
    pre:
      include:
        content:
          - "(?i)fakecloud"
    post:
      # Avoid matching values found on SourceGraph that look like dummy
      # passwords or insertions like:
      patternNot: 
        - "(\\w)\\1{6,}"
        - "(?i)rightcloud"
        - "(?i)testkey|(s|ex)ample""
  rules:
    # The RSpec Key corresponds to the contents of SonarSource/rspec.
    # To create a new secret, you need to create a new rule in rspec, which
    # will output a new rule ID (rspec key)
    - rspecKey: SXXXX
      id: fakecloud-api-key
      metadata:
        # This does not influence the rule name
        name: FakeCloud API Key

      detection:
        matching:
          # Matching guidelines: As a best practice, make the regexes as less
          # greedy as possible.
          # To do so, start to understand how many characters are in each part
          # of the secret. Avoid `+` or `*` for example.
          pattern: "(?i)\\b(fakecloud-\\w{32})\\b"

      # Add the compliant and non compliant example from the rspec as test cases
      # to make sure that they are detected correctly
      examples:
        - text: |
            SECRET=fakecloud-W1QrzMjSp5hYtO0KEvIU3C2dlx4GAfbn
          containsSecret: true
          match: fakecloud-W1QrzMjSp5hYtO0KEvIU3C2dlx4GAfbn
        - text: |
            BLABLA
          containsSecret: false
```

## RSpec writing

Depending on the impacts of leaking such a secret, pick [one of these impacts](https://github.com/SonarSource/rspec/tree/master/shared_content/secrets/impact).

a minimal version of a secrets rule description like that:

```
include::../../../shared_content/secrets/description.adoc[]

== Why is this an issue?

include::../../../shared_content/secrets/rationale.adoc[]

=== What is the potential impact?

Below are some real-world scenarios that illustrate some impacts of an attacker
exploiting the secret.

:secret_type: secret

include::../../../shared_content/secrets/impact/phishing.adoc[]

include::../../../shared_content/secrets/impact/malware_distribution.adoc[]

include::../../../shared_content/secrets/impact/suspicious_activities_termination.adoc[]

== How to fix it

include::../../../shared_content/secrets/fix/revoke.adoc[]

include::../../../shared_content/secrets/fix/vault.adoc[]

=== Code examples

:example_secret: fakecloud-5kl947611d0oagfbij4n23h3c58g2efm
:example_name: fakecloud-key
:example_env: FAKECLOUD_KEY

include::../../../shared_content/secrets/examples.adoc[]

//=== How does this work?

//=== Pitfalls

//=== Going the extra mile

== Resources

include::../../../shared_content/secrets/resources/standards.adoc[]

//=== Benchmarks
```

## Validation process

## Review Process

## Types of bugs that you will encounter

### SonarWay profile issues

When solving [Sonarway profile](https://github.com/SonarSource/sonar-text/blob/8a8ca0f4d5cb7ae484ccd297631c76a7d63a73b5/sonar-text-plugin/src/main/resources/org/sonar/l10n/secrets/rules/secrets/Sonar_way_profile.json)
issues, mind comas, and try not to remove rule keys. This might break master without notice


## Incomplete list of common patternNot patterns
Often it makes sense to match the patterns to filter out case insensitive `(?i)`.

If parts of the patternNot need to match case sensitive the case sensivity can be disabled via `(?-i)`.

Common patterns:

``` yaml
patternNot:
  # Character repeated X times like xxx, ..., \*\*\*
  # it is important to place this to the very beginning of the patternNot
  # because \1 will match the same text that was captured by the first capturing group
  - "([\\w\\*\\.])\\1{X,}"

  # Common text placeholders
  - "(?i)(?:s|ex)ample|foo|bar|test|abcd|redacted"
  - "(?i)^(\\$[a-z_]*)?(db|my)?_?pass(word|wd)?" 
    # <...>
  - "^<[^>]++>$"
  - "^<[\\w\\t -]{1,10}>?"
  - "^\\[[\\[\\w\\t \\-]+\\]$"
  - "^None$"

  # Common numeric placeholders
  - 1234(?:56)

  # Variables loaded from the environment:
  # E.g. os.getenv(...), os.environ[...], ENV(...), $env{...}
  # When this is used the main matching pattern should make sure to match
  # for default values. E.g. env("secret", default="default") should only match
  # 'default' so that this filter does not match 'env'. As an example:
  # (?:os\.getenv\(\s*+['\"][^'\"]++['\"]\s*+,\s*+)?['\"](match secret here)
  - "\\b(get)?env(iron)?\\b"

  # Other ways of loading from variables:
  # {{...}}
  - ^\{{2}[^}]++}{2}$
  - "\\$[({]\\w+(:-\\w+)?[})]"
  # `variable`
  - "`+[^`]*`+"
  # JS Template strings ${...}, Python template strings {...}
  - ^\$?\{[^}]++}$
  # Bash substitutions $...
  - ^\$[\w]$
  - "^\\$[a-z_]+pass(word)?$"
  # Ruby String Interpolation #{...}, #{...}#
  - ^#\{[^}]++}#?$
  #  $(...)
  - ^\$\([^)]++)$
  # Format string
  - "(?i)\b%s\b" 
```
