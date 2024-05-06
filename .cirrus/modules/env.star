# SHARED CANDIDATE
# ARTIFACTORY_DEPLOY_REPO_PRIVATE is added compared to Sonar IaC version: https://github.com/SonarSource/sonar-iac/blob/153aed5008efac5ff1bbb0014672e653194ee79b/.cirrus/modules/env.star#L2
def artifactory_env():
    """
    Provides typical environment variables to work with  Artifactory.

    The following credentials are provided:
    - private reader
    - qa deployer

    Most of the values are fetched from the Sonar Vault.

    :return: a dictionary with the following keys:
        - ARTIFACTORY_URL
        - ARTIFACTORY_PRIVATE_USERNAME
        - ARTIFACTORY_PRIVATE_PASSWORD
        - ARTIFACTORY_DEPLOY_USERNAME
        - ARTIFACTORY_DEPLOY_PASSWORD
        - ARTIFACTORY_DEPLOY_REPO
        - ARTIFACTORY_DEPLOY_REPO_PRIVATE
        - ARTIFACTORY_ACCESS_TOKEN
    """
    return {
        "ARTIFACTORY_URL": "VAULT[development/kv/data/repox data.url]",
        "ARTIFACTORY_PRIVATE_USERNAME": "vault-${CIRRUS_REPO_OWNER}-${CIRRUS_REPO_NAME}-private-reader",
        "ARTIFACTORY_PRIVATE_PASSWORD": "VAULT[development/artifactory/token/${CIRRUS_REPO_OWNER}-${CIRRUS_REPO_NAME}-private-reader access_token]",
        "ARTIFACTORY_DEPLOY_USERNAME": "vault-${CIRRUS_REPO_OWNER}-${CIRRUS_REPO_NAME}-qa-deployer",
        "ARTIFACTORY_DEPLOY_PASSWORD": "VAULT[development/artifactory/token/${CIRRUS_REPO_OWNER}-${CIRRUS_REPO_NAME}-qa-deployer access_token]",
        "ARTIFACTORY_DEPLOY_REPO": "sonarsource-public-qa",
        "ARTIFACTORY_DEPLOY_REPO_PRIVATE": "sonarsource-private-qa",
        "ARTIFACTORY_ACCESS_TOKEN": "VAULT[development/artifactory/token/${CIRRUS_REPO_OWNER}-${CIRRUS_REPO_NAME}-private-reader access_token]",
    }


# SHARED CANDIDATE
# Different from Sonar IaC version: https://github.com/SonarSource/sonar-iac/blob/153aed5008efac5ff1bbb0014672e653194ee79b/.cirrus/modules/env.star#L48
# Maybe CIRRUS_CLONE_DEPTH should be a parameter with a default value set to 1
def cirrus_env():
    """
    Provides typical environment variables to work with Cirrus CI.

    The following default values are provided:
    - CIRRUS_SHELL: bash
    - CIRRUS_CLONE_DEPTH: 1

    :return: a dictionary with the following keys:
        - CIRRUS_SHELL
        - CIRRUS_CLONE_DEPTH
    """
    return {
        "CIRRUS_SHELL": "bash",
        "CIRRUS_CLONE_DEPTH": 0,
    }


# SHARED CANDIDATE
def pgp_signing_env():
    """
    Provides the environment variables to sign artifacts with PGP.

    Values are fetched from the Sonar Vault.

    :return: a dictionary with the following keys:
        - SIGN_KEY
        - PGP_PASSPHRASE
    """
    return {
        "SIGN_KEY": "VAULT[development/kv/data/sign data.key]",
        "PGP_PASSPHRASE": "VAULT[development/kv/data/sign data.passphrase]",
    }


# SHARED CANDIDATE
# Same ORG_GRADLE_PROJECT_signingKeyId as Sonar IaC
# But repos might have different signing keys, a parameter could be added to the function
def gradle_signing_env():
    """
    Provides the environment variables to sign artifacts with Gradle.
    Values are fetched from the Sonar Vault.

    :return: a dictionary with the following keys:
        - ORG_GRADLE_PROJECT_signingKey
        - ORG_GRADLE_PROJECT_signingPassword
        - ORG_GRADLE_PROJECT_signingKeyId
    """
    return {
        "ORG_GRADLE_PROJECT_signingKey": "VAULT[development/kv/data/sign data.key]",
        "ORG_GRADLE_PROJECT_signingPassword": "VAULT[development/kv/data/sign data.passphrase]",
        "ORG_GRADLE_PROJECT_signingKeyId": "0x7DCD4258",
    }


# SHARED CANDIDATE
def whitesource_api_env():
    """
    Provides the environment variables to interact with the WhiteSource API.
    Values are fetched from the Sonar Vault.

    :return: a dictionary with the following keys:
        - WS_APIKEY
    """
    return {
        "WS_APIKEY": "VAULT[development/kv/data/mend data.apikey]"
    }


def gradle_env():
    """
    Provides typical environment variables to work with Gradle.
    The following default values are provided:
    - GRADLE_USER_HOME: ${CIRRUS_WORKING_DIR}/.gradle
    - GRADLE_COMMON_FLAGS: --console plain --no-daemon --profile

    :return: a dictionary with the following keys:
        - GRADLE_USER_HOME
        - GRADLE_COMMON_FLAGS
    """
    gradle_base = {
        "GRADLE_USER_HOME": "${CIRRUS_WORKING_DIR}/.gradle",
        "GRADLE_COMMON_FLAGS": "--console plain --no-daemon"
    }
    return gradle_base | gradle_signing_env()


# SHARED CANDIDATE
# Sonar Text uses Next whereas Sonar IaC uses SonarCloud, we need 2 distinct env var pairs
def next_env():
    """
    Provides typical environment variables to work with Next.
    The following default values are provided:
    - SONAR_HOST_URL: URL of the Next Sonar instance
    - SONAR_TOKEN: from the Sonar Vault

    :return: a dictionary with the following keys:
        - SONAR_HOST_URL
        - SONAR_TOKEN
    """
    return {
        "SONAR_HOST_URL": "VAULT[development/kv/data/next data.url]",
        "SONAR_TOKEN": "VAULT[development/kv/data/next data.token]",
    }


# SHARED CANDIDATE
def promotion_env():
    """
    Provides typical environment variables to promote artifacts.
    Values are fetched from the Sonar Vault.

    :return: a dictionary with the following keys:
        - ARTIFACTORY_PROMOTE_ACCESS_TOKEN
        - GITHUB_TOKEN
        - BURGR_URL
        - BURGR_USERNAME
        - BURGR_PASSWORD
    """
    return {
        "ARTIFACTORY_PROMOTE_ACCESS_TOKEN": "VAULT[development/artifactory/token/${CIRRUS_REPO_OWNER}-${CIRRUS_REPO_NAME}-promoter access_token]",
        "GITHUB_TOKEN": "VAULT[development/github/token/${CIRRUS_REPO_OWNER}-${CIRRUS_REPO_NAME}-promotion token]",
        "BURGR_URL": "VAULT[development/kv/data/burgr data.url]",
        "BURGR_USERNAME": "VAULT[development/kv/data/burgr data.cirrus_username]",
        "BURGR_PASSWORD": "VAULT[development/kv/data/burgr data.cirrus_password]",
    }


def env():
    vars = artifactory_env()
    vars |= cirrus_env()
    vars |= gradle_env()
    # REFACTOR: just-in-time secret injection?
    vars |= gradle_signing_env()
    vars |= next_env()
    return {"env": vars}
