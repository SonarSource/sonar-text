#!/bin/bash

# Assume the script is located in the root of the sonar-text repository
PROJECT_ROOT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

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

# Generated file where rspec keys to update are stored.
# This file will be generated at the secondStep of the generation of java classes
input="$PROJECT_ROOT_DIR/sonar-text-plugin/src/test/resources/templates/rspecKeysToUpdate.txt"

while IFS= read -r line
do
    echo ""
	echo "--------- Generating rspec files for: $line ---------"
	mvn exec:exec@generate --non-recursive -Penable-rule-api -Drules-metadata.directory=sonarpedia-secrets -DruleId="$line"
	echo ""
done < "$input"

rm "$PROJECT_ROOT_DIR/sonar-text-plugin/src/test/resources/templates/rspecKeysToUpdate.txt"

echo ""
echo "---------Finished Generation process---------"
echo "If the build process fails, this could be due to the enforcer-plugin."
echo "In this case set the maxSize (sonar-text-plugin/pom.xml) to a higher number"
