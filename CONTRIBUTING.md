# Contribute to the secrets of sonar-text

## Development environment

1. Set up a Java development environment
 - Or use Docker
2. Set up an extension that validates YAML files, such as [YAML by RedHat](https://github.com/redhat-developer/vscode-yaml)
3. Set up this [json-schema file](https://github.com/SonarSource/sonar-text/blob/master/sonar-text-plugin/src/main/resources/org/sonar/plugins/secrets/configuration/specifications/specification-json-schema.json) for your validation tooling
2. Take a look at [the existing rules](https://github.com/SonarSource/sonar-text/tree/master/sonar-text-plugin/src/main/resources/org/sonar/plugins/secrets/configuration)

## code structure for secret detection

Take a look at this example of a secret detection specification file for a fake cloud provider:

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
      # Avoid matching values found on SourceGraph that look like dummy passwords or insertions like:
      patternNot: 
        "(?i)\
        rightcloud|\
        (\\w)\\1{6,}|\
        testkey"
  rules:
    - rspecKey: SXXXX
      id: fakecloud-api-key
      metadata:
        name: FakeCloud API Key
      examples:
        - text: |
            BLABLA
          containsSecret: true
          match: XXXXX
        - text: |
            BLABLA
          containsSecret: false
      detection:
        matching:
          pattern: "(?i)\\b(fakecloud-\w{32})\\b"
```

## Types of bugs that you will encounter
