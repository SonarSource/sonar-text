#!/bin/bash

# Specification of the rules-api jar can be done in the call of this script as the first argument
if [[ "$1" ]]; then
    ruleapifilename=$1
else
    ruleapifilename="rule-api-snap.jar"
fi

echo ""
echo "--------- Generation of Java classes ---------"
echo ""
mvn test -Dtest=UpdatingSpecificationFilesGenerator#firstStep || exit

mvn test -Dtest=UpdatingSpecificationFilesGenerator#secondStep || exit

echo ""
echo "---------Creating current license headers---------"
echo ""
mvn license:format

echo ""
echo "---------Formatting code---------"
echo ""
mvn spotless:apply

cd sonarpedia-secrets

# Generated file where rspec keys to update are stored.
# This file will be generated at the secondStep of the generation of java classes
input="../sonar-text-plugin/src/test/resources/templates/rspecKeysToUpdate.txt"

while IFS= read -r line
do
    echo ""
	echo "--------- Generating rspec files for: $line ---------"
	java -jar ../$ruleapifilename generate -rule $line
	echo ""
done < "$input"

rm ../sonar-text-plugin/src/test/resources/templates/rspecKeysToUpdate.txt

echo ""
echo "---------Finished Generation process---------"
echo "If the build process fails, this could be due to the enforcer-plugin."
echo "In this case set the maxSize (sonar-text-plugin/pom.xml) to a higher number"
