load(
    "github.com/SonarSource/cirrus-modules/cloud-native/env.star@analysis/master",
    "promotion_env"
)
load("github.com/SonarSource/cirrus-modules/cloud-native/platform.star@analysis/master", "base_image_container_builder")
load("github.com/SonarSource/cirrus-modules/cloud-native/cache.star@analysis/master", "gradle_cache")

ARTIFACTS = [
    "org.sonarsource.text:sonar-text-plugin:jar",
    "com.sonarsource.text:sonar-text-enterprise-plugin:jar",
    "com.sonarsource.text:sonar-text-enterprise-plugin:yguard:xml",
]


# SHARED CANDIDATE???
# It's not clear what is using ARTIFACTS env variable but could be generic enough to be shared
def promote_env(artefacts=ARTIFACTS):
    env = promotion_env()
    env |= {"ARTIFACTS": ",".join(artefacts)}
    return env


# SHARED CANDIDATE???
# Not sure how generic this is but it could be shared
# Sonar IaC has a similar script but call a github notification in addition to Burgr: https://github.com/SonarSource/sonar-iac/blob/153aed5008efac5ff1bbb0014672e653194ee79b/.cirrus/modules/promote.star#L21
# In any case, parameterizing the script would be a good idea to make it more reusable
# It seems there is a core and options (notifications, ...)
def promote_script():
    return [
        "source cirrus-env PROMOTE",
        "cirrus_jfrog_promote multi",
        "burgr-notify-promotion"
    ]


# SHARED CANDIDATE???
# There are some specific configuration that might not be needed for all the projects
# TO CHECK : Sonar IaC does not use the cache, it it an issue? https://github.com/SonarSource/sonar-iac/blob/153aed5008efac5ff1bbb0014672e653194ee79b/.cirrus/modules/promote.star#L28
# Sonar Iac uses only_if https://github.com/SonarSource/sonar-iac/blob/153aed5008efac5ff1bbb0014672e653194ee79b/.cirrus/modules/promote.star#L31
def promote_task():
    return {
        "promote_task": {
            "depends_on": [
                "build",
                "qa_plugin",
                "sca_scan",
                "qa_os_win",
            ],
            "env": promote_env(),
            "gradle_cache": gradle_cache(),
            "eks_container": base_image_container_builder(cpu=1, memory="4G"),
            "script": promote_script()
        }
    }
