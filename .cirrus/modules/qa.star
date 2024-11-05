load("github.com/SonarSource/cirrus-modules/cloud-native/platform.star@analysis/master", "base_image_container_builder", "ec2_instance_builder")
load(
    "github.com/SonarSource/cirrus-modules/cloud-native/cache.star@analysis/master",
    "gradle_cache",
    "gradle_wrapper_cache",
    "cleanup_gradle_script",
    "orchestrator_cache",
    "set_orchestrator_home_script",
    "mkdir_orchestrator_home_script",
)
load(
    "github.com/SonarSource/cirrus-modules/cloud-native/actions.star@analysis/master",
    "default_gradle_on_failure",
)

QA_PLUGIN_GRADLE_TASK = "private:its:plugin:integrationTest"
QA_RULING_GRADLE_TASK = "private:its:ruling:integrationTest"
QA_BENCHMARK_GRADLE_TASK = "private:its:benchmark:integrationTest"
QA_QUBE_LATEST_RELEASE = "LATEST_RELEASE"


#
# Plugin
#

def qa_task(env, memory="11G", cpu="4"):
    return {
        "depends_on": "build",
        "eks_container": base_image_container_builder(memory=memory, cpu=cpu),
        "env": env,
        "gradle_cache": gradle_cache(),
        "gradle_wrapper_cache": gradle_wrapper_cache(),
        "set_orchestrator_home_script": set_orchestrator_home_script(),
        "mkdir_orchestrator_home_script": mkdir_orchestrator_home_script(),
        "orchestrator_cache": orchestrator_cache(),
        "run_its_script": run_its_script(),
        "on_failure": default_gradle_on_failure(),
        "cleanup_gradle_script": cleanup_gradle_script(),
    }


def run_its_script():
    return [
        "if [ \"$INIT_SUBMODULES\" == \"true\" ]; then git submodule update --init --depth 1; fi",
        "source cirrus-env QA",
        "source .cirrus/use-gradle-wrapper.sh",
        "./gradlew \"${GRADLE_TASK}\" \"-Dsonar.runtimeVersion=${SQ_VERSION}\" --info --build-cache --console plain --no-daemon"
    ]


# SHARED CANDIDATE?
# Almost the same as Sonar IaC but GITHUB_TOKEN is added: https://github.com/SonarSource/sonar-iac/blob/fbff28b4910ea545c1ed036690147cdb8b179302/.cirrus/modules/qa.star#L69
# This is because of orchestrator use
def qa_plugin_env():
    return {
        "GRADLE_TASK": QA_PLUGIN_GRADLE_TASK,
        "matrix": [
            {"SQ_VERSION": QA_QUBE_LATEST_RELEASE},
            {"SQ_VERSION": "DEV"},
        ],
        "GITHUB_TOKEN": "VAULT[development/github/token/licenses-ro token]",
    }


def qa_plugin_task():
    return {
        "qa_plugin_task": qa_task(qa_plugin_env(), memory="12G", cpu="6")
    }


#
# Ruling
#

# SHARED CANDIDATE?
# Almost the same as Sonar IaC but GITHUB_TOKEN is added: https://github.com/SonarSource/sonar-iac/blob/fbff28b4910ea545c1ed036690147cdb8b179302/.cirrus/modules/qa.star#L89
# This is because of orchestrator use
def qa_ruling_env():
    return {
        "GRADLE_TASK": QA_RULING_GRADLE_TASK,
        "SQ_VERSION": QA_QUBE_LATEST_RELEASE,
        "GITHUB_TOKEN": "VAULT[development/github/token/licenses-ro token]",
    }


def qa_ruling_task():
    return {
        "qa_ruling_task": qa_task(qa_ruling_env())
    }


def qa_benchmark_condition():
    return "$CIRRUS_PR_LABELS !=~ \".*qa-bench.*\""


def qa_benchmark_env():
    return {
        "GRADLE_TASK": QA_BENCHMARK_GRADLE_TASK,
        "GITHUB_TOKEN": "VAULT[development/github/token/licenses-ro token]",
        "SQ_VERSION": "LATEST_RELEASE",
        "BENCHMARK_SETTINGS": "$CIRRUS_PR_LABELS",
        "INIT_SUBMODULES": "true"
    }


def qa_benchmark_task():
    return {
        "qa_benchmark_task": {
            "depends_on": [
                "build",
                "qa_plugin",
                "qa_ruling"
            ],
            "skip": qa_benchmark_condition(),
            "env": qa_benchmark_env(),
            "eks_container": base_image_container_builder(memory="14G", cpu="16"),
            "gradle_cache": gradle_cache(),
            "gradle_wrapper_cache": gradle_wrapper_cache(),
            "set_orchestrator_home_script": set_orchestrator_home_script(),
            "mkdir_orchestrator_home_script": mkdir_orchestrator_home_script(),
            "orchestrator_cache": orchestrator_cache(),
            "run_benchmark_script": run_its_script(),
            "cleanup_gradle_script": cleanup_gradle_script(),
            "on_failure": default_gradle_on_failure(),
        }
    }

def qa_win_condition():
    return "$CIRRUS_PR_LABELS =~ \".*qa-win.*\" || $CIRRUS_BRANCH == $CIRRUS_DEFAULT_BRANCH || $CIRRUS_BRANCH =~ \"branch-.*\""

def qa_win_env():
    return {
        "SQ_VERSION": QA_QUBE_LATEST_RELEASE,
        "GITHUB_TOKEN": "VAULT[development/github/token/licenses-ro token]",
    }

def qa_win_script():
    return [
        "source cirrus-env CI",
        "./gradlew ${GRADLE_COMMON_FLAGS} --info --no-parallel --build-cache test integrationTest -x :private:its:benchmark:integrationTest",
    ]

def qa_os_win_task():
    return {
        "qa_os_win_task": {
            "depends_on": "build",
            "only_if": qa_win_condition(),
            "env": qa_win_env(),
            "ec2_instance": ec2_instance_builder(),
            "gradle_cache": gradle_cache(fingerprint_script="git rev-parse HEAD"),
            "gradle_wrapper_cache": gradle_wrapper_cache(),
            "set_orchestrator_home_script": set_orchestrator_home_script(),
            "mkdir_orchestrator_home_script": mkdir_orchestrator_home_script(),
            "orchestrator_cache": orchestrator_cache(),
            "build_script": qa_win_script(),
            "cleanup_gradle_script": cleanup_gradle_script(),
            "on_failure": default_gradle_on_failure(),
        }
    }
