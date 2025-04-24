load(
    "github.com/SonarSource/cirrus-modules/cloud-native/env.star@analysis/master",
    "gradle_signing_env",
    "pgp_signing_env",
    "whitesource_api_env"
)
load("github.com/SonarSource/cirrus-modules/cloud-native/conditions.star@analysis/master", "is_main_branch")
load("github.com/SonarSource/cirrus-modules/cloud-native/platform.star@analysis/master", "base_image_container_builder")
load(
    "github.com/SonarSource/cirrus-modules/cloud-native/cache.star@analysis/master",
    "gradle_cache",
    "gradle_wrapper_cache",
    "cleanup_gradle_script",
    "project_version_cache",
    "store_project_version_script"
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
        "BUILD_ARGUMENTS": "--profile"
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
            "eks_container": base_image_container_builder(cpu=4, memory="8G"),
            "project_version_cache": project_version_cache(),
            "gradle_cache": gradle_cache(),
            "gradle_wrapper_cache": gradle_wrapper_cache(),
            "build_script": build_script(),
            "cleanup_gradle_script": cleanup_gradle_script(),
            "store_project_version_script": store_project_version_script(),
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
