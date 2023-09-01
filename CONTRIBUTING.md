# Contribute to the secrets of sonar-text

## Development environment

1. Set up a Java development environment, or use Docker, to be ready to perform commands such as those specified in the [README.md](README.md)
2. Set up an extension that validates YAML files, such as [YAML by RedHat](https://github.com/redhat-developer/vscode-yaml)
3. Set up this [json-schema file](https://github.com/SonarSource/sonar-text/blob/master/sonar-text-plugin/src/main/resources/org/sonar/plugins/secrets/configuration/specifications/specification-json-schema.json) for your validation tooling
4. Take a look at [the existing rules](https://github.com/SonarSource/sonar-text/tree/master/sonar-text-plugin/src/main/resources/org/sonar/plugins/secrets/configuration)
5. Familiarize yourself with [SonarSource/rspec](https://github.com/SonarSource/rspec)

## code structure for secret detection

Take a look at the example of a secret detection specification file for a fake cloud provider below. This is considered the anatomy of a "good" secret detection file.

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
        "(?i)\
        rightcloud|\
        (\\w)\\1{6,}|\
        testkey"
  rules:
    # The RSpec Key corresponds to the contents of SonarSource/rspec.
    # To create a new secret, you need to create a new rule in rspec, which
    # will output a new rule ID (rspec key)
    - rspecKey: SXXXX
      id: fakecloud-api-key
      metadata:
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
            BLABLA
          containsSecret: true
          match: XXXXX
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

## Types of bugs that you will encounter

## Incomplete list of common patternNot patterns
Often it makes sense to match the patterns to filter out case insensitive ```(?i)```.
If parts of the patternNot need to match case sensitive the case sensivity can be anabled again via ```(?-i)```

Common patterns:
```
# Repeating characters like xxx, ..., ***
# it is important to place this to the very beginning of the patternNot
# because \1 will match the same text that was captured by the first capturing group
([\w\*\.])\1{2,}
# Variables loaded from the environment:
# E.g. os.getenv(...), os.environ[...], ENV(...), $env{...}
# TODO: Improve this to still raise for env("secret", default="default")

\b(get)?env(iron)?\b
# Other ways of loading from variables:
# {{...}}
^\{{2}[^}]++}{2}$
# JS Template strings ${...}, Python template strings {...}
^\$?\{[^}]++}$
# Bash substitutions $...
^\$[\w]$
# Ruby String Interpolation #{...}, #{...}#
^#\{[^}]++}#?$
#  $(...)
^\$\([^)]++)$
# Format string
\b%s\b
# <...>
^<[^>]++>$
# Common placeholders
1234
foo
bar
```
