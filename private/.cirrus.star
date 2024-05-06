# Check Starlark specs: https://github.com/bazelbuild/starlark/blob/master/spec.md

load("../.cirrus/modules/helper.star", "merge_dict")


def env():
    return {
        "env": {
            "ARTIFACTORY_URL": "VAULT[development/kv/data/repox data.url]",
            "ARTIFACTORY_PRIVATE_USERNAME": "VAULT[development/artifactory/token/${CIRRUS_REPO_OWNER}-${CIRRUS_REPO_NAME}-private-reader username]",
            "ARTIFACTORY_PRIVATE_PASSWORD": "VAULT[development/artifactory/token/${CIRRUS_REPO_OWNER}-${CIRRUS_REPO_NAME}-private-reader access_token]",
            "ARTIFACTORY_ACCESS_TOKEN": "VAULT[development/artifactory/token/${CIRRUS_REPO_OWNER}-${CIRRUS_REPO_NAME}-private-reader access_token]",
            "ARTIFACTORY_DEPLOY_USERNAME": "VAULT[development/artifactory/token/${CIRRUS_REPO_OWNER}-${CIRRUS_REPO_NAME}-qa-deployer username]",
            "ARTIFACTORY_DEPLOY_PASSWORD": "VAULT[development/artifactory/token/${CIRRUS_REPO_OWNER}-${CIRRUS_REPO_NAME}-qa-deployer access_token]",
            "ARTIFACTORY_DEPLOY_REPO": "sonarsource-public-qa",
            "ARTIFACTORY_DEPLOY_REPO_PRIVATE": "sonarsource-private-qa",
            "SONAR_TOKEN": "VAULT[development/kv/data/next data.token]",
            "SONAR_HOST_URL": "VAULT[development/kv/data/next data.url]",
            "CIRRUS_SHELL": "bash",
            "CIRRUS_CLONE_DEPTH": 0,
            "ORG_GRADLE_PROJECT_signingKey": "VAULT[development/kv/data/sign data.key]",
            "ORG_GRADLE_PROJECT_signingPassword": "VAULT[development/kv/data/sign data.passphrase]",
            "ORG_GRADLE_PROJECT_signingKeyId": "0x7DCD4258",
            "GRADLE_USER_HOME": "${CIRRUS_WORKING_DIR}/.gradle",
            "GRADLE_COMMON_FLAGS": "--console plain --no-daemon"
        }
    }


def build_task():
    return {
        "build_task": {
            "env": {
                "ARTIFACTORY_USER": "$ARTIFACTORY_PRIVATE_USERNAME",
                "ARTIFACTORY_PASSWORD": "$ARTIFACTORY_PRIVATE_PASSWORD",
                "SIGN_KEY": "VAULT[development/kv/data/sign data.key]",
                "PGP_PASSPHRASE": "VAULT[development/kv/data/sign data.passphrase]",
                "DEPLOY_PULL_REQUEST": "true"
            },
            "gradle_cache": {
                "folder": ".gradle/caches",
                "fingerprint_script": "git rev-parse HEAD"
            },
            "create_gradle_directory_script": [
                "mkdir -p \"${CIRRUS_WORKING_DIR}/.gradle\""
            ],
            "eks_container": {
                "cluster_name": "${CIRRUS_CLUSTER_NAME}",
                "region": "eu-central-1",
                "namespace": "default",
                "use_in_memory_disk": "true",
                "image": "${CIRRUS_AWS_ACCOUNT}.dkr.ecr.eu-central-1.amazonaws.com/base:j17-latest",
                "cpu": 4,
                "memory": "8G"
            },
            "build_script": [
                "source cirrus-env BUILD-PRIVATE",
                "source .cirrus/use-gradle-wrapper.sh",
                "regular_gradle_build_deploy_analyze"
            ],
            "cleanup_gradle_script": [
                "/usr/bin/find \"${CIRRUS_WORKING_DIR}/.gradle/caches/\" -name \"*.lock\" -type f -delete",
                "/usr/bin/find \"${CIRRUS_WORKING_DIR}/.gradle/caches\" -type d -name \"8.*\" -prune -maxdepth 1 -exec rm -rf \"{}\" \\;",
                "rm -rf \"${CIRRUS_WORKING_DIR}/.gradle/caches/journal-1/\"",
                "rm -rf ~/.gradle/caches/transforms-1"
            ],
        }
    }


def ws_scan_task():
    return {
        "ws_scan_task": {
            "depends_on": [
                "build"
            ],
            "only_if": "$CIRRUS_USER_COLLABORATOR == 'true' && $CIRRUS_TAG == \"\" && ($CIRRUS_BRANCH == $CIRRUS_DEFAULT_BRANCH || $CIRRUS_BRANCH =~ \"branch-.*\")",
            "env": {
                "WS_APIKEY": "VAULT[development/kv/data/mend data.apikey]"
            },
            "eks_container": {
                "cluster_name": "${CIRRUS_CLUSTER_NAME}",
                "region": "eu-central-1",
                "namespace": "default",
                "use_in_memory_disk": "true",
                "image": "${CIRRUS_AWS_ACCOUNT}.dkr.ecr.eu-central-1.amazonaws.com/base:j17-latest",
                "cpu": 1,
                "memory": "4G"
            },
            "create_gradle_directory_script": [
                "mkdir -p \"${CIRRUS_WORKING_DIR}/.gradle\""
            ],
            "gradle_cache": {
                "folder": ".gradle/caches",
                "fingerprint_script": "git rev-parse HEAD"
            },
            "whitesource_script": [
                "source cirrus-env QA",
                "GRADLE_OPTS=\"-Xmx64m -Dorg.gradle.jvmargs='-Xmx3G' -Dorg.gradle.daemon=false\" ./gradlew ${GRADLE_COMMON_FLAGS} :sonar-text-plugin:processResources -Pkotlin.compiler.execution.strategy=in-process",
                "source ./export_ws_variables.sh",
                "source ws_scan.sh"
            ],
            "cleanup_gradle_script": [
                "/usr/bin/find \"${CIRRUS_WORKING_DIR}/.gradle/caches/\" -name \"*.lock\" -type f -delete",
                "/usr/bin/find \"${CIRRUS_WORKING_DIR}/.gradle/caches\" -type d -name \"8.*\" -prune -maxdepth 1 -exec rm -rf \"{}\" \\;",
                "rm -rf \"${CIRRUS_WORKING_DIR}/.gradle/caches/journal-1/\"",
                "rm -rf ~/.gradle/caches/transforms-1"
            ],
            "allow_failures": "true",
            "always": {
                "ws_artifacts": {
                    "path": "whitesource/**/*"
                }
            }
        }
    }


def qa_plugin_task():
    return {
        "qa_plugin_task": {
            "depends_on": [
                "build"
            ],
            "env": {
                "GRADLE_TASK": "private:its:plugin:integrationTest",
                "matrix": [
                    {
                        "SQ_VERSION": "LATEST_RELEASE"
                    },
                    {
                        "SQ_VERSION": "DEV"
                    }
                ],
                "GITHUB_TOKEN": "VAULT[development/github/token/licenses-ro token]"
            },
            "eks_container": {
                "cluster_name": "${CIRRUS_CLUSTER_NAME}",
                "region": "eu-central-1",
                "namespace": "default",
                "use_in_memory_disk": "true",
                "image": "${CIRRUS_AWS_ACCOUNT}.dkr.ecr.eu-central-1.amazonaws.com/base:j17-latest",
                "cpu": 4,
                "memory": "10G"
            },
            "gradle_cache": {
                "folder": ".gradle/caches",
                "fingerprint_script": "git rev-parse HEAD"
            },
            "create_gradle_directory_script": [
                "mkdir -p \"${CIRRUS_WORKING_DIR}/.gradle\""
            ],
            "set_orchestrator_home_script": "export TODAY=$(date '+%Y-%m-%d')\necho \"TODAY=${TODAY}\" >> $CIRRUS_ENV\necho \"ORCHESTRATOR_HOME=${CIRRUS_WORKING_DIR}/orchestrator/${TODAY}\" >> $CIRRUS_ENV\n",
            "mkdir_orchestrator_home_script": "echo \"Create dir ${ORCHESTRATOR_HOME} if needed\"\nmkdir -p ${ORCHESTRATOR_HOME}\n",
            "orchestrator_cache": {
                "folder": "${ORCHESTRATOR_HOME}",
                "fingerprint_script": "echo ${TODAY}",
                "reupload_on_changes": "true"
            },
            "run_its_script": [
                "if [ \"$INIT_SUBMODULES\" == \"true\" ]; then git submodule update --init --depth 1; fi",
                "source cirrus-env QA",
                "./gradlew ${GRADLE_COMMON_FLAGS} --info --build-cache -Dsonar.runtimeVersion=${SQ_VERSION} ${GRADLE_TASK}"
            ],
            "cleanup_gradle_script": [
                "/usr/bin/find \"${CIRRUS_WORKING_DIR}/.gradle/caches/\" -name \"*.lock\" -type f -delete",
                "/usr/bin/find \"${CIRRUS_WORKING_DIR}/.gradle/caches\" -type d -name \"8.*\" -prune -maxdepth 1 -exec rm -rf \"{}\" \\;",
                "rm -rf \"${CIRRUS_WORKING_DIR}/.gradle/caches/journal-1/\"",
                "rm -rf ~/.gradle/caches/transforms-1"
            ],
            "on_failure": {
                "reports_artifacts": {
                    "path": "**/build/reports/**/*"
                },
                "junit_artifacts": {
                    "path": "**/test-results/**/*.xml",
                    "format": "junit"
                }
            },
        }
    }


def qa_ruling_task():
    return {
        "qa_ruling_task": {
            "depends_on": [
                "build"
            ],
            "env": {
                "GRADLE_TASK": "private:its:ruling:integrationTest",
                "GITHUB_TOKEN": "VAULT[development/github/token/licenses-ro token]",
                "SQ_VERSION": "LATEST_RELEASE"
            },
            "create_gradle_directory_script": [
                "mkdir -p \"${CIRRUS_WORKING_DIR}/.gradle\""
            ],
            "gradle_cache": {
                "folder": ".gradle/caches",
                "fingerprint_script": "git rev-parse HEAD"
            },
            "set_orchestrator_home_script": "export TODAY=$(date '+%Y-%m-%d')\necho \"TODAY=${TODAY}\" >> $CIRRUS_ENV\necho \"ORCHESTRATOR_HOME=${CIRRUS_WORKING_DIR}/orchestrator/${TODAY}\" >> $CIRRUS_ENV\n",
            "mkdir_orchestrator_home_script": "echo \"Create dir ${ORCHESTRATOR_HOME} if needed\"\nmkdir -p ${ORCHESTRATOR_HOME}\n",
            "orchestrator_cache": {
                "folder": "${ORCHESTRATOR_HOME}",
                "fingerprint_script": "echo ${TODAY}",
                "reupload_on_changes": "true"
            },
            "eks_container": {
                "cluster_name": "${CIRRUS_CLUSTER_NAME}",
                "region": "eu-central-1",
                "namespace": "default",
                "use_in_memory_disk": "true",
                "image": "${CIRRUS_AWS_ACCOUNT}.dkr.ecr.eu-central-1.amazonaws.com/base:j17-latest",
                "cpu": 4,
                "memory": "10G"
            },
            "run_its_script": [
                "if [ \"$INIT_SUBMODULES\" == \"true\" ]; then git submodule update --init --depth 1; fi",
                "source cirrus-env QA",
                "./gradlew ${GRADLE_COMMON_FLAGS} --info --build-cache -Dsonar.runtimeVersion=${SQ_VERSION} ${GRADLE_TASK}"
            ],
            "cleanup_gradle_script": [
                "/usr/bin/find \"${CIRRUS_WORKING_DIR}/.gradle/caches/\" -name \"*.lock\" -type f -delete",
                "/usr/bin/find \"${CIRRUS_WORKING_DIR}/.gradle/caches\" -type d -name \"8.*\" -prune -maxdepth 1 -exec rm -rf \"{}\" \\;",
                "rm -rf \"${CIRRUS_WORKING_DIR}/.gradle/caches/journal-1/\"",
                "rm -rf ~/.gradle/caches/transforms-1"
            ],
            "on_failure": {
                "reports_artifacts": {
                    "path": "**/build/reports/**/*"
                },
                "junit_artifacts": {
                    "path": "**/test-results/**/*.xml",
                    "format": "junit"
                }
            },
        }
    }


def qa_benchmark_task():
    return {
        "qa_benchmark_task": {
            "depends_on": [
                "build",
                "qa_plugin",
                "qa_ruling"
            ],
            "skip": "$CIRRUS_PR_LABELS !=~ \".*qa-bench.*\" || $CIRRUS_BRANCH == $CIRRUS_DEFAULT_BRANCH || $CIRRUS_BRANCH =~ \"branch-.*\"",
            "env": {
                "GRADLE_TASK": "private:its:benchmark:integrationTest",
                "GITHUB_TOKEN": "VAULT[development/github/token/licenses-ro token]",
                "SQ_VERSION": "LATEST_RELEASE",
                "BENCHMARK_SETTINGS": "$CIRRUS_PR_LABELS",
                "INIT_SUBMODULES": "true"
            },
            "eks_container": {
                "cluster_name": "${CIRRUS_CLUSTER_NAME}",
                "region": "eu-central-1",
                "namespace": "default",
                "use_in_memory_disk": "true",
                "image": "${CIRRUS_AWS_ACCOUNT}.dkr.ecr.eu-central-1.amazonaws.com/base:j17-latest",
                "cpu": 16,
                "memory": "14G"
            },
            "create_gradle_directory_script": [
                "mkdir -p \"${CIRRUS_WORKING_DIR}/.gradle\""
            ],
            "gradle_cache": {
                "folder": ".gradle/caches",
                "fingerprint_script": "git rev-parse HEAD"
            },
            "set_orchestrator_home_script": "export TODAY=$(date '+%Y-%m-%d')\necho \"TODAY=${TODAY}\" >> $CIRRUS_ENV\necho \"ORCHESTRATOR_HOME=${CIRRUS_WORKING_DIR}/orchestrator/${TODAY}\" >> $CIRRUS_ENV\n",
            "mkdir_orchestrator_home_script": "echo \"Create dir ${ORCHESTRATOR_HOME} if needed\"\nmkdir -p ${ORCHESTRATOR_HOME}\n",
            "orchestrator_cache": {
                "folder": "${ORCHESTRATOR_HOME}",
                "fingerprint_script": "echo ${TODAY}",
                "reupload_on_changes": "true"
            },
            "run_its_script": [
                "if [ \"$INIT_SUBMODULES\" == \"true\" ]; then git submodule update --init --depth 1; fi",
                "source cirrus-env QA",
                "./gradlew ${GRADLE_COMMON_FLAGS} --info --build-cache -Dsonar.runtimeVersion=${SQ_VERSION} ${GRADLE_TASK}"
            ],
            "cleanup_gradle_script": [
                "/usr/bin/find \"${CIRRUS_WORKING_DIR}/.gradle/caches/\" -name \"*.lock\" -type f -delete",
                "/usr/bin/find \"${CIRRUS_WORKING_DIR}/.gradle/caches\" -type d -name \"8.*\" -prune -maxdepth 1 -exec rm -rf \"{}\" \\;",
                "rm -rf \"${CIRRUS_WORKING_DIR}/.gradle/caches/journal-1/\"",
                "rm -rf ~/.gradle/caches/transforms-1"
            ],
            "on_failure": {
                "reports_artifacts": {
                    "path": "**/build/reports/**/*"
                },
                "junit_artifacts": {
                    "path": "**/test-results/**/*.xml",
                    "format": "junit"
                }
            },
        }
    }


def promote_task():
    return {
        "promote_task": {
            "depends_on": [
                "build",
                "qa_plugin",
                "ws_scan"
            ],
            "env": {
                "ARTIFACTORY_PROMOTE_ACCESS_TOKEN": "VAULT[development/artifactory/token/${CIRRUS_REPO_OWNER}-${CIRRUS_REPO_NAME}-promoter access_token]",
                "GITHUB_TOKEN": "VAULT[development/github/token/${CIRRUS_REPO_OWNER}-${CIRRUS_REPO_NAME}-promotion token]",
                "BURGR_URL": "VAULT[development/kv/data/burgr data.url]",
                "BURGR_USERNAME": "VAULT[development/kv/data/burgr data.cirrus_username]",
                "BURGR_PASSWORD": "VAULT[development/kv/data/burgr data.cirrus_password]",
                "ARTIFACTS": "org.sonarsource.text:sonar-text-plugin:jar,com.sonarsource.text:sonar-text-enterprise-plugin:jar,com.sonarsource.text:sonar-text-enterprise-plugin:yguard:xml"
            },
            "create_gradle_directory_script": [
                "mkdir -p \"${CIRRUS_WORKING_DIR}/.gradle\""
            ],
            "gradle_cache": {
                "folder": ".gradle/caches",
                "fingerprint_script": "git rev-parse HEAD"
            },
            "eks_container": {
                "cluster_name": "${CIRRUS_CLUSTER_NAME}",
                "region": "eu-central-1",
                "namespace": "default",
                "use_in_memory_disk": "true",
                "image": "${CIRRUS_AWS_ACCOUNT}.dkr.ecr.eu-central-1.amazonaws.com/base:j17-latest",
                "cpu": 1,
                "memory": "4G"
            },
            "script": [
                "source cirrus-env PROMOTE",
                "cirrus_jfrog_promote multi",
                "burgr-notify-promotion"
            ]
        }
    }


def private_pipeline_builder():
    conf = dict()
    merge_dict(conf, env())
    merge_dict(conf, build_task())
    merge_dict(conf, ws_scan_task())
    merge_dict(conf, qa_plugin_task())
    merge_dict(conf, qa_ruling_task())
    merge_dict(conf, qa_benchmark_task())
    merge_dict(conf, promote_task())
    return conf
