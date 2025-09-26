load(
    "github.com/SonarSource/cirrus-modules/cloud-native/env.star@analysis/master",
    "promotion_env"
)
load("github.com/SonarSource/cirrus-modules/cloud-native/platform.star@analysis/master", "base_image_container_builder")
load("github.com/SonarSource/cirrus-modules/cloud-native/cache.star@analysis/master", "project_version_cache")
load("github.com/SonarSource/cirrus-modules/cloud-native/conditions.star@analysis/master", "is_branch_qa_eligible")


# SHARED CANDIDATE???
# Not sure how generic this is but it could be shared
# It seems there is a core and options (notifications, ...)
def promote_script():
    return [
        "source cirrus-env PROMOTE",
        "cirrus_jfrog_promote multi",
        "export PROJECT_VERSION=$(cat ${PROJECT_VERSION_CACHE_DIR}/evaluated_project_version.txt)",
        "github-notify-promotion",
    ]


# SHARED CANDIDATE???
# There are some specific configuration that might not be needed for all the projects
# TO CHECK : Sonar IaC does not use the cache, it it an issue? https://github.com/SonarSource/sonar-iac/blob/153aed5008efac5ff1bbb0014672e653194ee79b/.cirrus/modules/promote.star#L28
# Sonar Iac uses only_if https://github.com/SonarSource/sonar-iac/blob/153aed5008efac5ff1bbb0014672e653194ee79b/.cirrus/modules/promote.star#L31
def promote_task():
    return {
        "promote_task": {
            "only_if": is_branch_qa_eligible(),
            "depends_on": [
                "build",
                "publish_artifacts",
                "qa_plugin",
                "qa_os_win",
            ],
            "env": promotion_env(),
            "eks_container": base_image_container_builder(cpu=1, memory="4G"),
            "project_version_cache": project_version_cache(),
            "script": promote_script()
        }
    }
