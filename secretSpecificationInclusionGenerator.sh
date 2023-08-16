#!/bin/bash

if [[ "$1" ]]; then
    ruleapifilename=$1
else
    ruleapifilename="rule-api-snap.jar"
fi

echo ""
echo "--------- Generation of Java classes ---------"
echo ""
mvn test -Dtest=UpdatingSpecificationFilesGenerator#firstStep test

mvn test -Dtest=UpdatingSpecificationFilesGenerator#secondStep test

echo ""
echo "---------Creating current license headers---------"
echo ""
mvn license:format

echo ""
echo "---------Formatting code---------"
echo ""
mvn spotless:apply

cd sonarpedia-secrets

# generated file where rspec keys to update are stored
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
echo "If the build process fails, this could be due to the enfore-plugin."
echo "In this case set the maxSize (sonar-text-plugin/pom.xml) to a higher number"