#!/bin/bash
# Exit immediately if a command fail
set -e

echo ""
echo "--------- Generation of Java classes ---------"
echo ""
./gradlew updateCheckClasses || exit

echo ""
echo "---------Creating current license headers and formatting code---------"
echo ""
./gradlew spotlessApply

if [ -z "$1" ]
then
  echo "--------- Generating rspec files for all rules from branch: 'master' ---------"
  ./gradlew ruleApiGenerateRuleGeneration
else
  echo "--------- Generating rspec files for all rules from branch: '$1' ---------"
  ./gradlew ruleApiGenerateRuleGeneration -Pbranch="$1"
fi

echo ""
echo "---------Finished Generation process---------"
echo "If the build process fails, this could be due to the enforcer-plugin."
echo "In this case set the maxSize (sonar-text-plugin/pom.xml) to a higher number"
