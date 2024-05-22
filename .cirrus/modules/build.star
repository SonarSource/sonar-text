load(
    "github.com/SonarSource/cirrus-modules/cloud-native/env.star@analysis/master",
    "pgp_signing_env",
    "whitesource_api_env"
)
load("conditions.star", "is_main_branch")
load("platform.star", "base_image_container_builder")
load(
    "cache.star",
    "gradle_cache",
    "cleanup_gradle_script",
    "orchestrator_cache",
    "set_orchestrator_home_script",
    "mkdir_orchestrator_home_script",
)


# SHARED CANDIDATE?
# Looks like : https://github.com/SonarSource/sonar-iac/blob/153aed5008efac5ff1bbb0014672e653194ee79b/.cirrus/modules/build.star#L48
def build_env():
    env = pgp_signing_env()
    env |= {
        "DEPLOY_PULL_REQUEST": "true",
        "BUILD_ARGUMENTS": "-DtrafficInspection=false --parallel --profile -x test -x sonar"
    }
    return env


# SHARED CANDIDATE?
# Sonar Text has two sides, private and public and it seems it introduces some complexity in the build
# Which in turn makes it hard to share the build script with a standard gradle build like Sonar IaC
def build_script():
    return [
        "source cirrus-env BUILD-PRIVATE",
        "source .cirrus/use-gradle-wrapper.sh",
        "regular_gradle_build_deploy_analyze ${BUILD_ARGUMENTS}"
    ]


# Different from Sonar IaC from many aspects: on_success, project_version related conf, ...
def build_task():
    return {
        "build_task": {
            "env": build_env(),
            "gradle_cache": gradle_cache(),
            "eks_container": base_image_container_builder(cpu=4, memory="8G"),
            "build_script": build_script(),
            "cleanup_gradle_script": cleanup_gradle_script(),
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
        "source cirrus-env QA",
        "GRADLE_OPTS=\"-Xmx64m -Dorg.gradle.jvmargs='-Xmx3G' -Dorg.gradle.daemon=false\" ./gradlew ${GRADLE_COMMON_FLAGS} :sonar-text-plugin:processResources -Pkotlin.compiler.execution.strategy=in-process",
        "source ./export_ws_variables.sh",
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
