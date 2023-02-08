#$signAssembly = "$env:CIRRUS_BRANCH" -eq "master" -or "$env:CIRRUS_BRANCH".startsWith("branch-")
$signAssembly = "true"
Write-Host "Should the assembly be signed: $signAssembly"

Write-Host "Executing unit tests"
if($signAssembly)
{
  dotnet test "${env:PROJECT_DIR}" -p:AltCover=true,AltCoverForce=true,AltCoverVisibleBranches=true,AltCoverAssemblyFilter="Moq",AltCoverReport=coverage.xml,AltCoverStrongNameKey=${env:SNK_PATH} --configuration ${env:BUILD_CONFIGURATION} --no-build
}
else
{
  dotnet test "${env:PROJECT_DIR}" -p:AltCover=true,AltCoverForce=true,AltCoverVisibleBranches=true,AltCoverAssemblyFilter="Moq",AltCoverReport=coverage.xml --configuration ${env:BUILD_CONFIGURATION} --no-build
}

if(!$?) {
  Write-Host "sonar-text-dotnet unit tests failed."
  Exit $LASTEXITCODE
}

Write-Host "Executing integration tests"
dotnet test "${env:PROJECT_DIR}\\src\\IntegrationTests\\IntegrationTests.sln" -c ${env:BUILD_CONFIGURATION} -p:SignAssembly=${signAssembly}
if(!$?) {
  Write-Host "sonar-text-dotnet integration tests failed."
  Exit $LASTEXITCODE
}