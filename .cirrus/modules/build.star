load(
    "github.com/SonarSource/cirrus-modules/cloud-native/env.star@analysis/master",
    "gradle_signing_env",
    "pgp_signing_env"
)
load("github.com/SonarSource/cirrus-modules/cloud-native/conditions.star@analysis/master", "is_main_branch", "is_branch_qa_eligible")
load("github.com/SonarSource/cirrus-modules/cloud-native/platform.star@analysis/master",
     "base_image_container_builder",
     "ec2_instance_builder",
     "arm64_container_builder")
load(
    "github.com/SonarSource/cirrus-modules/cloud-native/cache.star@analysis/master",
    "gradle_cache",
    "gradle_wrapper_cache",
    "cleanup_gradle_script",
    "project_version_cache"
)
load(
    "github.com/SonarSource/cirrus-modules/cloud-native/actions.star@analysis/master",
    "default_gradle_on_failure",
    "gradle_junit_xml_report_artifacts"
)

# SHARED CANDIDATE?
# Looks like : https://github.com/SonarSource/sonar-iac/blob/153aed5008efac5ff1bbb0014672e653194ee79b/.cirrus/modules/build.star#L48
def build_env():
    env = pgp_signing_env()
    env |= gradle_signing_env()
    env |= {
        "DEPLOY_PULL_REQUEST": "true",
        "BUILD_ARGUMENTS": "--profile storeProjectVersion -x artifactoryPublish",
        "SONAR_PROJECT_KEY": "org.sonarsource.text:text"
    }
    return env


# SHARED CANDIDATE?
# Sonar Text has two sides, private and public and it seems it introduces some complexity in the build
# Which in turn makes it hard to share the build script with a standard gradle build like Sonar IaC
def build_script():
    return [
        "git submodule update --init --depth 1 -- build-logic/common",
        "source cirrus-env BUILD-PRIVATE",
        "source .cirrus/use-gradle-wrapper.sh",
        "regular_gradle_build_deploy_analyze :build-logic-text:test ${BUILD_ARGUMENTS}",
    ]


# Different from Sonar IaC from many aspects: on_success, project_version related conf, ...
def build_task():
    return {
        "build_task": {
            "env": build_env(),
            "eks_container": base_image_container_builder(cpu=4, memory="10G"),
            "project_version_cache": project_version_cache(),
            "gradle_cache": gradle_cache(),
            "gradle_wrapper_cache": gradle_wrapper_cache(),
            "build_script": build_script(),
            "cleanup_gradle_script": cleanup_gradle_script(),
            "on_success": {
                "profile_report_artifacts": {
                    "path": "build/reports/profile/profile-*.html"
                }
            },
            "on_failure": {
                "junit_artifacts": gradle_junit_xml_report_artifacts(),
                "flatten_report_script": [
                    # language=bash
                    """
                    find . -type d -path \\*/build/reports/tests | while read -r test_dir; do
                        project_name=$(basename $(realpath ${test_dir}/../../..))
                        mkdir -p ${project_name}-test-report
                        cp -r "${test_dir}"/test/* ${project_name}-test-report
                    done
                    """
                ],
                "flattened_report_artifacts": {
                    "path": "*-test-report/**"
                }
            }
        }
    }


# To avoid overusing expensive MacOS nodes, we limit native CLI builds to main branches and PRs with a specific label
def build_native_cli_condition():
    return "$CIRRUS_PR_LABELS =~ \".*build-cli-native.*\" || $CIRRUS_BRANCH == $CIRRUS_DEFAULT_BRANCH || $CIRRUS_BRANCH =~ \"branch-.*\""


GRAALVM_VERSION = "24.0.2"
GRAALVM_CE_OPENJDK_VERSION = "24.0.2+11.1"


# Task template suitable for systems with Bash (Linux, MacOS)
def build_cli_task_template(install_graalvm_script=[
    "mkdir -p ${GRADLE_USER_HOME}/jdks/",
    "curl --proto \"=https\" -sSfL -O https://github.com/graalvm/graalvm-ce-builds/releases/download/jdk-{}/graalvm-community-jdk-{}_linux-x64_bin.tar.gz".format(GRAALVM_VERSION, GRAALVM_VERSION),
    "tar -xzf graalvm-community-jdk-{}_linux-x64_bin.tar.gz --directory ${{GRADLE_USER_HOME}}/jdks/".format(GRAALVM_VERSION),
    "rm graalvm-community-jdk-{}_linux-x64_bin.tar.gz".format(GRAALVM_VERSION),
], build_script=[
    # Common command for Linux and MacOS
    "./gradlew -DbuildNumber=\"${CI_BUILD_NUMBER}\" :private:sonar-secrets-cli:test :private:sonar-secrets-cli:nativeCompile --info",
]):
    return {
        "only_if": build_native_cli_condition(),
        "depends_on": "build",
        "env": build_env() | {
            "JAVA_HOME": "${{GRADLE_USER_HOME}}/jdks/graalvm-community-openjdk-{}".format(GRAALVM_CE_OPENJDK_VERSION),
            "GRAALVM_HOME": "${{GRADLE_USER_HOME}}/jdks/graalvm-community-openjdk-{}".format(GRAALVM_CE_OPENJDK_VERSION),
            "BUILD_NATIVE_IMAGE": "true",
        },
        "prepare_script": [
            "git submodule update --init --depth 1 -- build-logic/common",
        ],
        "install_graalvm_script": install_graalvm_script,
        "build_script": build_script,
        "on_success": {
            "sonar_secrets_cli_artifacts": {
                "path": "private/sonar-secrets-cli/build/native/nativeCompile/sonar-secrets*"
            }
        },
        "on_failure": default_gradle_on_failure(),
    }


def build_cli_linux_task():
    return {
        "build_cli_linux_task": build_cli_task_template() | {
           "eks_container": base_image_container_builder(cpu=8, memory="16G"),
            "gradle_cache": gradle_cache(),
            "gradle_wrapper_cache": gradle_wrapper_cache(),
        }
    }


def build_cli_win_task():
    return {
        "build_cli_win_task": {
            "env": {
                # peachee-windows-dotnet doesn't have Bash, so scripts have to work in cmd.exe
                "CIRRUS_SHELL": "cmd.exe",
                "DEPLOY_PULL_REQUEST": "true",
                "GRADLE_USER_HOME": "C:\\Windows\\SystemTemp\\cirrus-ci-build\\.gradle",
                "JAVA_HOME": "%GRADLE_USER_HOME%\\jdks\\graalvm-community-openjdk-{}".format(GRAALVM_CE_OPENJDK_VERSION),
                "GRAALVM_HOME": "%GRADLE_USER_HOME%\\jdks\\graalvm-community-openjdk-{}".format(GRAALVM_CE_OPENJDK_VERSION),
                "BUILD_NATIVE_IMAGE": "true",
            },
            "only_if": build_native_cli_condition(),
            "depends_on": "build",
            # GraalVM Native Image on Windows requires MSVS 2022, which is already present in the Dotnet image
            "ec2_instance": ec2_instance_builder(image="peachee-windows-dotnet-v*"),
            "prepare_script": [
                "git submodule update --init --depth 1 -- build-logic/common",
            ],
            "install_graalvm_script": [
                "mkdir %GRADLE_USER_HOME%\\jdks",
                "curl --proto \"=https\" -sSfL -O https://github.com/graalvm/graalvm-ce-builds/releases/download/jdk-{}/graalvm-community-jdk-{}_windows-x64_bin.zip".format(GRAALVM_VERSION, GRAALVM_VERSION),
                "tar -xf graalvm-community-jdk-{}_windows-x64_bin.zip -C %GRADLE_USER_HOME%\\jdks".format(GRAALVM_VERSION),
                "del graalvm-community-jdk-{}_windows-x64_bin.zip".format(GRAALVM_VERSION),
            ],
            "build_script": [
                "gradlew.bat -DbuildNumber=%CI_BUILD_NUMBER% :private:sonar-secrets-cli:test :private:sonar-secrets-cli:nativeCompile --info",
            ],
            "on_success": {
                "sonar_secrets_cli_artifacts": {
                    "path": "private\\sonar-secrets-cli\\build\\native\\nativeCompile\\sonar-secrets*"
                }
            },
        }
    }


def build_cli_macos_task():
    return {
        "build_cli_macos_task": build_cli_task_template(install_graalvm_script=[
            "mkdir -p ${{GRADLE_USER_HOME}}/jdks/graalvm-community-openjdk-{}".format(GRAALVM_CE_OPENJDK_VERSION),
            "curl --proto \"=https\" -sSfL -O https://github.com/graalvm/graalvm-ce-builds/releases/download/jdk-{}/graalvm-community-jdk-{}_macos-aarch64_bin.tar.gz".format(GRAALVM_VERSION, GRAALVM_VERSION),
            "tar -xzf graalvm-community-jdk-{}_macos-aarch64_bin.tar.gz graalvm-community-openjdk-{}/Contents/Home".format(GRAALVM_VERSION, GRAALVM_CE_OPENJDK_VERSION),
            "mv graalvm-community-openjdk-{}/Contents/Home/* ${{GRADLE_USER_HOME}}/jdks/graalvm-community-openjdk-{}".format(GRAALVM_CE_OPENJDK_VERSION, GRAALVM_CE_OPENJDK_VERSION),
            "rm graalvm-community-jdk-{}_macos-aarch64_bin.tar.gz".format(GRAALVM_VERSION),
            "rm -r graalvm-community-openjdk-{}".format(GRAALVM_CE_OPENJDK_VERSION),
        ]) | {
            "persistent_worker": {
                "isolation": {
                    "tart": {
                        "image": "ghcr.io/cirruslabs/macos-sonoma-xcode:latest",
                        "cpu": 3,
                        "memory": "6G",
                    },
                    "resources": {
                        "tart-vms": 1,
                    },
                    "labels": {
                        "envname": "prod",
                    }
                }
            }
        }
    }


def publish_artifacts_task():
    artifact_zip_name = "sonar_secrets_cli.zip"
    return {
        "publish_artifacts_task": {
            "depends_on": [
                "build",
                "build_cli_linux",
                "build_cli_win",
                "build_cli_macos",
            ],
            "env": {
                "CIRRUS_TOKEN": "VAULT[development/kv/data/cirrusci/github/SonarSource data.api_token]",
            } | gradle_signing_env(),
            "eks_container": base_image_container_builder(cpu=4, memory="10G"),
            "project_version_cache": project_version_cache(),
            "gradle_cache": gradle_cache(),
            "gradle_wrapper_cache": gradle_wrapper_cache(),
            "download_cli_artifacts_script": [
                "curl -SL -H \"Authorization: Bearer $CIRRUS_TOKEN\" -O https://api.cirrus-ci.com/v1/artifact/build/${{CIRRUS_BUILD_ID}}/{} || exit 0".format(artifact_zip_name),
                "mkdir -p private/sonar-secrets-cli/build/tmp/",
                "mkdir -p private/sonar-secrets-cli/build/native/images/",
                "test -f {} && unzip {} -d private/sonar-secrets-cli/build/tmp/ || echo Archive not found, skipping".format(artifact_zip_name, artifact_zip_name),
                "find private/sonar-secrets-cli/build/tmp/ -type f -exec mv {} private/sonar-secrets-cli/build/native/images/ \\;",
                "ls -l private/sonar-secrets-cli/build/native/images/",
            ],
            "publish_script": [
                "git submodule update --init --depth 1 -- build-logic/common",
                "./gradlew -DbuildNumber=\"${CI_BUILD_NUMBER}\" artifactoryPublish -PnativeImagesDir=build/native/images/ --info",
            ],
            "cleanup_gradle_script": cleanup_gradle_script(),
        }
    }


#
# Shadow Scans
#

def is_run_shadow_scan():
    return "($CIRRUS_CRON == $CRON_NIGHTLY_JOB_NAME && $CIRRUS_BRANCH == \"master\") || $CIRRUS_PR_LABELS =~ \".*shadow_scan.*\""

def shadow_scan_general_env():
    env = pgp_signing_env()
    env |= gradle_signing_env()
    env |= {
        "DEPLOY_PULL_REQUEST": "false",
        "BUILD_ARGUMENTS": "-x artifactoryPublish --no-parallel",
        "CRON_NIGHTLY_JOB_NAME": "nightly",
        "SONAR_PROJECT_KEY": "SonarSource_sonar-text-enterprise"
    }
    return env

def shadow_scan_task_template(env):
    return {
        "only_if": "({}) && ({})".format(is_branch_qa_eligible(), is_run_shadow_scan()),
        "depends_on": "build",
        "env": env,
        "eks_container": base_image_container_builder(cpu=4, memory="10G"),
        "project_version_cache": project_version_cache(),
        "gradle_cache": gradle_cache(),
        "gradle_wrapper_cache": gradle_wrapper_cache(),
        "build_script": build_script(),
        "cleanup_gradle_script": cleanup_gradle_script(),
    }


def shadow_scan_sqc_eu_env():
    env = shadow_scan_general_env()
    env |= {
       "SONAR_TOKEN": "VAULT[development/kv/data/sonarcloud data.token]",
       "SONAR_HOST_URL": "https://sonarcloud.io"
    }
    return env

# Overlap with build_task, because of imminent GHA migration we don't take any effort to abstract it
def shadow_scan_sqc_eu_task():
    return {
        "shadow_scan_sqc_eu_task": shadow_scan_task_template(shadow_scan_sqc_eu_env())
    }

def shadow_scan_sqc_us_env():
    env = shadow_scan_general_env()
    env |= {
       "SONAR_TOKEN": "VAULT[development/kv/data/sonarqube-us data.token]",
       "SONAR_HOST_URL": "https://sonarqube.us"
    }
    return env

# Overlap with build_task, because of imminent GHA migration we don't take any effort to abstract it
def shadow_scan_sqc_us_task():
    return {
        "shadow_scan_sqc_us_task": shadow_scan_task_template(shadow_scan_sqc_us_env())
    }


#
# Iris
#

def iris_general_env():
    return {
       "SONAR_SOURCE_IRIS_TOKEN": "VAULT[development/kv/data/iris data.next]",
       "SONAR_SOURCE_PROJECT_KEY": "org.sonarsource.text:text",
       "CRON_NIGHTLY_JOB_NAME": "nightly",
    }

def run_iris_task_template(env):
    return {
        "only_if": "({}) && ({})".format(is_branch_qa_eligible(), is_run_shadow_scan()),
        "depends_on": "promote",
        "env": env,
        "eks_container": base_image_container_builder(cpu=2, memory="2G"),
        "build_script": [
                        "./run_iris.sh"
                    ],
    }

# Next Enterprise -> SQC EU Enterprise

def iris_next_enterprise_to_sqc_eu_enterprise_env():
    env = iris_general_env()
    env |= {
       "SONAR_TARGET_URL": "https://sonarcloud.io",
       "SONAR_TARGET_IRIS_TOKEN": "VAULT[development/kv/data/iris data.sqc-eu]",
       "SONAR_TARGET_PROJECT_KEY": "SonarSource_sonar-text-enterprise",
    }
    return env

def run_iris_next_enterprise_to_sqc_eu_enterprise_task():
    return {
        "run_iris_next_enterprise_to_sqc_eu_enterprise_task": run_iris_task_template(iris_next_enterprise_to_sqc_eu_enterprise_env())
    }

# Next Enterprise -> SQC EU Public

def iris_next_enterprise_to_sqc_eu_public_env():
    env = iris_general_env()
    env |= {
       "SONAR_TARGET_URL": "https://sonarcloud.io",
       "SONAR_TARGET_IRIS_TOKEN": "VAULT[development/kv/data/iris data.sqc-eu]",
       "SONAR_TARGET_PROJECT_KEY": "org.sonarsource.text:text",
    }
    return env

def run_iris_next_enterprise_to_sqc_eu_public_task():
    return {
        "run_iris_next_enterprise_to_sqc_eu_public_task": run_iris_task_template(iris_next_enterprise_to_sqc_eu_public_env())
    }

# Next Enterprise -> SQC US Enterprise

def iris_next_enterprise_to_sqc_us_enterprise_env():
    env = iris_general_env()
    env |= {
       "SONAR_TARGET_URL": "https://sonarqube.us",
       "SONAR_TARGET_IRIS_TOKEN": "VAULT[development/kv/data/iris data.sqc-us]",
       "SONAR_TARGET_PROJECT_KEY": "SonarSource_sonar-iac-enterprise",
    }
    return env

def run_iris_next_enterprise_to_sqc_us_enterprise_task():
    return {
        "run_iris_next_enterprise_to_sqc_us_enterprise_task": run_iris_task_template(iris_next_enterprise_to_sqc_us_enterprise_env())
    }

# Next Enterprise -> SQC US Public

def iris_next_enterprise_to_sqc_us_public_env():
    env = iris_general_env()
    env |= {
       "SONAR_TARGET_URL": "https://sonarqube.us",
       "SONAR_TARGET_IRIS_TOKEN": "VAULT[development/kv/data/iris data.sqc-eu]",
       "SONAR_TARGET_PROJECT_KEY": "SonarSource_sonar-iac",
    }
    return env

def run_iris_next_enterprise_to_sqc_us_public_task():
    return {
        "run_iris_next_enterprise_to_sqc_us_public_task": run_iris_task_template(iris_next_enterprise_to_sqc_us_public_env())
    }

# Next Enterprise -> Next Public

def iris_next_enterprise_to_next_public_env():
    env = iris_general_env()
    env |= {
       "SONAR_TARGET_URL": "https://next.sonarqube.com/sonarqube",
       "SONAR_TARGET_IRIS_TOKEN": "VAULT[development/kv/data/iris data.next]",
       "SONAR_TARGET_PROJECT_KEY": "SonarSource_sonar-iac",
    }
    return env

def run_iris_next_enterprise_to_next_public_task():
    return {
        "run_iris_next_enterprise_to_next_public_task": run_iris_task_template(iris_next_enterprise_to_next_public_env())
    }
