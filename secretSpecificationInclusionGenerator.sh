#!/bin/bash
# Exit immediately if a command fail
set -e

# Assume the script is located in the root of the sonar-text repository
PROJECT_ROOT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

echo ""
echo "--------- Generation of Java classes ---------"
echo ""
./gradlew :sonar-text-plugin:updateCheckClasses || exit

echo ""
echo "---------Creating current license headers and formatting code---------"
echo ""
./gradlew spotlessApply

# Generated file where rspec keys to update are stored.
# This file is generated during previous steps
input="$PROJECT_ROOT_DIR/sonar-text-plugin/build/generated/rspecKeysToUpdate.txt"

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

rm "$PROJECT_ROOT_DIR/sonar-text-plugin/build/generated/rspecKeysToUpdate.txt"

echo ""
echo "---------Finished Generation process---------"
echo "If the build process fails, this could be due to the enforcer-plugin."
echo "In this case set the maxSize (sonar-text-plugin/pom.xml) to a higher number"
