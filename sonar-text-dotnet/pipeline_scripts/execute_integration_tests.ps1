$signAssembly = "$env:CIRRUS_BRANCH" -eq "master" -or "$env:CIRRUS_BRANCH".startsWith("branch-")
Write-Host "Should the assembly be signed: $signAssembly"
dotnet test "${env:PROJECT_DIR}\\src\\IntegrationTests\\IntegrationTests.sln" -c ${env:BUILD_CONFIGURATION} -p:SignAssembly=${signAssembly}
if(!$?) {
  Write-Host "sonar-text-dotnet integration tests failed."
  Exit $LASTEXITCODE
}