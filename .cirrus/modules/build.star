load(
    "github.com/SonarSource/cirrus-modules/cloud-native/env.star@analysis/master",
    "gradle_signing_env",
    "pgp_signing_env",
    "whitesource_api_env"
)
load("github.com/SonarSource/cirrus-modules/cloud-native/conditions.star@analysis/master", "is_main_branch", "is_branch_qa_eligible")
load("github.com/SonarSource/cirrus-modules/cloud-native/platform.star@analysis/master", "base_image_container_builder")
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
        "BUILD_ARGUMENTS": "--profile storeProjectVersion",
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


#
# Whitesource scan
#
# SHARED CANDIDATE???
# Some bits depend on the project: memory options, gradle task
# Different from Sonar IaC from many aspects but it is justified?
# In any case, parameterizing the script would be a good idea to make it more reusable
def whitesource_script():
    return [
        "git submodule update --init --depth 1 -- build-logic/common",
        "source cirrus-env QA",
        "source .cirrus/use-gradle-wrapper.sh",
        "export PROJECT_VERSION=$(cat ${PROJECT_VERSION_CACHE_DIR}/evaluated_project_version.txt)",
        "GRADLE_OPTS=\"-Xmx64m -Dorg.gradle.jvmargs='-Xmx3G' -Dorg.gradle.daemon=false\" ./gradlew ${GRADLE_COMMON_FLAGS} :sonar-text-plugin:processResources -Pkotlin.compiler.execution.strategy=in-process",
        "source ws_scan.sh",
    ]


# SHARED CANDIDATE???
# Some bits depend on the project: project version cache, on success profile report artifacts
# on_success is activated for Sonar IaC : https://github.com/SonarSource/sonar-iac/blob/153aed5008efac5ff1bbb0014672e653194ee79b/.cirrus/modules/build.star#L135
# Project version cache is used in Sonar IaC: https://github.com/SonarSource/sonar-iac/blob/153aed5008efac5ff1bbb0014672e653194ee79b/.cirrus/modules/build.star#L126
def sca_scan_task():
    return {
        "sca_scan_task": {
            "only_if": is_main_branch(),
            "depends_on": "build",
            "env": whitesource_api_env(),
            "eks_container": base_image_container_builder(cpu=1, memory="4G"),
            "gradle_cache": gradle_cache(),
            "gradle_wrapper_cache": gradle_wrapper_cache(),
            "project_version_cache": project_version_cache(),
            "whitesource_script": whitesource_script(),
            "cleanup_gradle_script": cleanup_gradle_script(),
            "allow_failures": "true",
            "always": {
                "ws_artifacts": {
                    "path": "whitesource/**/*"
                }
            },
        }
    }
