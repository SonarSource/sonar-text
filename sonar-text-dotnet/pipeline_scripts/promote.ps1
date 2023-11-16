Set-Location "$env:CIRRUS_WORKING_DIR\\sonar-text-dotnet"

Write-Host "Setting up artifactory authentication for promotion"
jf config add "repox" --url https://repox.jfrog.io --access-token $env:ARTIFACTORY_PROMOTE_ACCESS_TOKEN --overwrite
jf rt ping # Check if the artifactory configuration is successfull

$isMasterOrBranchBuild  = "$env:CIRRUS_BRANCH" -eq "master" -or "$env:CIRRUS_BRANCH".startsWith("branch-")
$sourceRepo = "sonarsource-nuget-qa"
$destinationRepo = if ($isMasterOrBranchBuild) { "sonarsource-nuget-public" } else { "sonarsource-nuget-dev" }

Write-Host "Promoting the NuGet package from $sourceRepo to $destinationRepo"
jf rt build-promote $env:ARTIFACTORY_BUILD_NAME $env:BUILD_NUMBER $destinationRepo --source-repo $sourceRepo
if(!$?) {
  Write-Host "sonar-text-dotnet promote step failed."
  Exit $LASTEXITCODE
}
