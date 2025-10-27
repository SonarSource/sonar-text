# Contribute to the secrets of sonar-text

## Development environment

* Set up an extension that validates YAML files, such as [YAML by RedHat](https://github.com/redhat-developer/vscode-yaml)
* Set up this [json-schema file](https://github.com/SonarSource/sonar-text/blob/master/sonar-text-plugin/src/main/resources/org/sonar/plugins/secrets/configuration/specifications/specification-json-schema.json) for your validation tooling
* Take a look at [the existing rules](https://github.com/SonarSource/sonar-text/tree/master/sonar-text-plugin/src/main/resources/org/sonar/plugins/secrets/configuration)

## Adding a secret

### Common errors (potentially outdated)

- Modifying code from the GitHub UI and not validating it with the schema
  - It will raise tons of exceptions, and `"DeserializationException: Could not load specification from file: slack.yaml"`  
- Mismatch from what's expected in `rules[X].examples[X].match` and what's raised  (if `containsSecret` is set to true):
  - `java.lang.AssertionError: actual is not empty while group of values to look for is.`
- If `containsSecret` is set to false but something is still raised
  - `Expecting empty but was: [DefaultIssue[ruleKey=secrets:SXXXXX,gap=<null>,overriddenSeverity=<null>,quickFixAvailable=false,ruleDescriptionContextKey=<null>,codeVariants=<null>,...,saved=true]]`
- Creating overly greedy regex. In any case we now have tests checking for problematic regexes.
- Escaping:
  - `pre.include.content`: Should not be escaped
  - `pattern` regex: Should be escaped: 
    - Tokens starting with backslashes should have another backslash
  
  
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
        - "(?<char>\\w)\\k<char>{6,}"
        - "(?i)rightcloud"
        - "(?i)testkey|(s|ex)ample"
  rules:
    # The RSpec Key corresponds to the contents of SonarSource/rspec.
    # To create a new secret, you need to create a new rule in rspec, which
    # will output a new rule ID (rspec key)
    - rspecKey: SXXXX
      id: fakecloud-api-key
      # The `selectivity` field can be either `specific`, `providerGeneric` or `analyzerGeneric`.
      # In case two rules match on the same text,
      # the issue with the more specific selectivity will be used with the following order `specific` > `providerGeneric` > `analyzerGeneric`.
      # Rules with a selectivity of `analyzerGeneric` will also only be raised on files without a sonar language associated to it. 
      selectivity: specific
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
            foobar
          containsSecret: false
```

### Issue location
By default, the entire region matched by `matching.pattern` is highlighted as an issue. If pattern contains capturing groups (doesn't matter if they are named or not), the first capturing group is used to highlight the issue. So, if you are adding capturing groups (for example, to add a post filter for a named group representing part of a secret), make sure to also surround the entire pattern with parentheses. Otherwise, only the first capturing group will be highlighted.

### Reusing common blocks in secret specification
You can reuse common blocks in the secret specification files. Common blocks are placed in separate files in the `common` directory of the `plugins/secrets` directory. A common block is represented as `${common/fileName.yaml}`; it will be substituted with the content of the file during build time. The block will have the same indentation as the `${common/fileName.yaml}` line.

For example, if you have a common `patternNot` block that you want to use across multiple specifications, you can define it once and reference it in your specifications.

``` yaml
# common/patternNot.yaml
- "(?<char>\\w)\\k<char>{6,}"
- "(?i)rightcloud"
- "(?i)testkey|(s|ex)ample"
```

Then, in your specification file, you can reference it like this:

```yaml
provider:
  detection:
    post:
      patternNot:
        ${common/patternNot.yaml}
```

Note: resulting full YAML files are generated during build time and stored in the `build/resources/main` directory. During debugging, you can check the generated files to see what the final output looks like.

## RSpec writing

Depending on the impacts of leaking such a secret, pick [one of these impacts](https://github.com/SonarSource/rspec/tree/master/shared_content/secrets/impact).

Here is an example of a rule description for a secret provider:

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



## Incomplete list of common patternNot patterns
Often it makes sense to match the patterns to filter out case insensitive `(?i)`.

If parts of the patternNot need to match case sensitive the case sensivity can be disabled via `(?-i)`.

Common patterns:

``` yaml
patternNot:
  # Character repeated X times like xxx, ..., \*\*\*
  # it is important to use named capturing groups and not numbered capturing groups here
  # because this pattern may end-up in a concatenated regex pattern with other capturing groups
  - "(?<char>[\\w\\*\\.])\\k<char>{X,}"

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
  # {{...}} and python f-strings
  - ^\{{2}[^}]++}{2}$
  - "\\$[({]\\w+(:-\\w+)?[})]"
  # `variable`
  - "`+[^`]*`+"
  # JS Template strings ${...}, Python template strings {...}
  - ^\$?\{[^}]++}$
  # Bash substitutions $..., $$... when it is escaped (in docker compose file for example)
  - ^\${1,2}[\w]$
  - "^\\${1,2}[a-z_]+pass(word)?$"
  # Ruby String Interpolation #{...}, #{...}#
  - ^#\{[^}]++}#?$
  #  $(...)
  - ^\$\([^)]++)$
  # Format string
  - "(?i)\b%s\b" 
```
