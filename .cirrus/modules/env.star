load(
    "github.com/SonarSource/cirrus-modules/cloud-native/env.star@analysis/master",
    "artifactory_env",
    "cirrus_env",
    "gradle_signing_env",
    "next_env",
    "gradle_env"
)


def env():
    vars = artifactory_env()
    vars |= cirrus_env()
    vars |= gradle_env()
    # REFACTOR: just-in-time secret injection?
    vars |= gradle_signing_env()
    vars |= next_env()
    return {"env": vars}
