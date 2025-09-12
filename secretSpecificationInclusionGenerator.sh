#!/bin/bash
# Exit immediately if a command fail
set -e

# Function to show usage
show_usage() {
    >&2 echo "Usage: ./secretSpecificationInclusionGenerator.sh <branch> <rulekey> [--public]"
    >&2 echo ""
    >&2 echo "Arguments:"
    >&2 echo "  <branch>   The branch to use for generation"
    >&2 echo "  <rulekey>  The rule key to generate"
    >&2 echo ""
    >&2 echo "Options:"
    >&2 echo "  --public   Use ruleApiGenerateRuleSecrets for public version"
    >&2 echo "             (default: use ruleApiGenerateRuleDeveloperSecrets for developer version)"
}

# Check minimum number of arguments
if (( $# < 2 )); then
    show_usage
    exit 1
fi

# Parse arguments
BRANCH="$1"
RULEKEY="$2"
PUBLIC_FLAG=false

# Check for optional --public flag
if (( $# >= 3 )); then
    if [[ "$3" == "--public" ]]; then
        PUBLIC_FLAG=true
    else
        >&2 echo "Error: Unknown argument '$3'"
        show_usage
        exit 1
    fi
fi

# Determine gradle task based on flag
if [[ "$PUBLIC_FLAG" == true ]]; then
    GRADLE_TASK="ruleApiGenerateRuleSecrets"
    VERSION_TYPE="public"
else
    GRADLE_TASK="ruleApiGenerateRuleDeveloperSecrets"
    VERSION_TYPE="developer"
fi

echo ""
echo "--------- Generation of Java classes ---------"
echo ""
./gradlew updateCheckClasses || exit

echo ""
echo "---------Creating current license headers and formatting code---------"
echo ""
./gradlew spotlessApply

echo "--------- Generating rspec files for all rules from branch: '$BRANCH' using $GRADLE_TASK ($VERSION_TYPE version) with rule $RULEKEY ---------"
./gradlew $GRADLE_TASK -Pbranch="$BRANCH" -Prule=$RULEKEY

echo ""
echo "---------Finished Generation process---------"
echo "If the build process fails, this could be due to the enforcer-plugin."
echo "In this case set the maxSize (sonar-text-plugin/pom.xml) to a higher number"
