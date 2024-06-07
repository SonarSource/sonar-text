#!/bin/bash
# Exit immediately if a command fail
set -e

# Assume the script is located in the root of the sonar-text repository
PROJECT_ROOT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

echo ""
echo "--------- Generation of Java classes ---------"
echo ""
GENERATION_ENABLED=true ./gradlew :sonar-text-plugin:test --tests UpdatingSpecificationFilesGenerator.secondStep || exit

echo ""
echo "---------Creating current license headers and formatting code---------"
echo ""
./gradlew spotlessApply

# Generated file where rspec keys to update are stored.
# This file will be generated at the secondStep of the generation of java classes
input="$PROJECT_ROOT_DIR/sonar-text-plugin/src/test/resources/templates/rspecKeysToUpdate.txt"

while IFS= read -r line
do
  echo ""
  if [ -z "$1" ]
  then
  	echo "--------- Generating rspec files for '$line' from branch: 'master' ---------"
    ./gradlew ruleApiGenerateRuleSecrets -Prule="$line"
  else
    echo "--------- Generating rspec files for '$line' from branch: '$1' ---------"
    ./gradlew ruleApiGenerateRuleSecrets -Prule="$line" -Pbranch="$1"
  fi
	echo ""
done < "$input"

rm "$PROJECT_ROOT_DIR/sonar-text-plugin/src/test/resources/templates/rspecKeysToUpdate.txt"

echo ""
echo "---------Finished Generation process---------"
echo "If the build process fails, this could be due to the enforcer-plugin."
echo "In this case set the maxSize (sonar-text-plugin/pom.xml) to a higher number"
