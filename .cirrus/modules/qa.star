load("platform.star", "base_image_container_builder")
load(
    "cache.star",
    "gradle_cache",
    "cleanup_gradle_script",
    "orchestrator_cache",
    "set_orchestrator_home_script",
    "mkdir_orchestrator_home_script",
)

QA_PLUGIN_GRADLE_TASK = "private:its:plugin:integrationTest"
QA_RULING_GRADLE_TASK = "private:its:ruling:integrationTest"
QA_BENCHMARK_GRADLE_TASK = "private:its:benchmark:integrationTest"
QA_QUBE_LATEST_RELEASE = "LATEST_RELEASE"


def on_failure():
    return {
        "reports_artifacts": {
            "path": "**/build/reports/**/*"
        },
        "junit_artifacts": {
            "path": "**/test-results/**/*.xml",
            "format": "junit"
        }
    }


#
# Plugin
#

def qa_task(env, memory="10G", cpu="4"):
    return {
        "depends_on": "build",
        "eks_container": base_image_container_builder(memory=memory, cpu=cpu),
        "env": env,
        "gradle_cache": gradle_cache(),
        "set_orchestrator_home_script": set_orchestrator_home_script(),
        "mkdir_orchestrator_home_script": mkdir_orchestrator_home_script(),
        "orchestrator_cache": orchestrator_cache(),
        "run_its_script": run_its_script(),
        "cleanup_gradle_script": cleanup_gradle_script(),
        "on_failure": on_failure(),
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
        "qa_plugin_task": qa_task(qa_plugin_env())
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
    return "$CIRRUS_PR_LABELS !=~ \".*qa-bench.*\" || $CIRRUS_BRANCH == $CIRRUS_DEFAULT_BRANCH || $CIRRUS_BRANCH =~ \"branch-.*\""


def qa_benchmark_env():
    return {
        "GRADLE_TASK": QA_BENCHMARK_GRADLE_TASK,
        "GITHUB_TOKEN": "VAULT[development/github/token/licenses-ro token]",
        "SQ_VERSION": "LATEST_RELEASE",
        "BENCHMARK_SETTINGS": "$CIRRUS_PR_LABELS",
        "INIT_SUBMODULES": "true"
    }


def run_benchmark_script():
    return [
        "if [ \"$INIT_SUBMODULES\" == \"true\" ]; then git submodule update --init --depth 1; fi",
        "source cirrus-env QA",
        "./gradlew ${GRADLE_COMMON_FLAGS} --info --build-cache -Dsonar.runtimeVersion=${SQ_VERSION} ${GRADLE_TASK}"
    ]


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
            "set_orchestrator_home_script": set_orchestrator_home_script(),
            "mkdir_orchestrator_home_script": mkdir_orchestrator_home_script(),
            "orchestrator_cache": orchestrator_cache(),
            "run_benchmark_script": run_benchmark_script(),
            "cleanup_gradle_script": cleanup_gradle_script(),
            "on_failure": on_failure(),
        }
    }
