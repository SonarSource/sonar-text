function CheckIfSuccessful($StepName) {
  if(!$?) {
    Write-Host "sonar-text-dotnet $StepName failed."
    Exit $LASTEXITCODE
  }
}

Set-Location $env:PROJECT_DIR

# In order to allow trackable builds in artifactory, we need to supply the `build-name` and `build-number` to nuget and dotnet commands.
$buildId = $env:BUILD_NUMBER
$buildName = $env:ARTIFACTORY_BUILD_NAME
$nugetFeed = "https://repox.jfrog.io/artifactory/api/nuget/nuget"

Write-Host "Setting up artifactory authentication for QA deploy"
jf config add "repox" --url https://repox.jfrog.io --access-token $env:ARTIFACTORY_DEPLOY_PASSWORD
jf rt ping # Check if the artifactory configuration is successfull

Write-Host "Setting up artifactory dotnet configuration"
jf dotnet-config --global --server-id-resolve repox --repo-resolve $nugetFeed

#$signAssembly = "$env:CIRRUS_BRANCH" -eq "master" -or "$env:CIRRUS_BRANCH".startsWith("branch-")
$signAssembly = "true"
Write-Host "Should the assembly be signed: $signAssembly"

Write-Host "Locating SignTool.exe"
$searchRoot = ${env:ProgramFiles(x86)} + '\Windows Kits\10\bin\10*'
$exeName = 'signtool.exe'
$signtool = Get-ChildItem -Path $searchRoot -Filter $exeName -Recurse -ErrorAction SilentlyContinue -Force | Select -Last 1
if (!$signtool)
{
throw 'Unable to find ' + $exeName + ' under ' + $searchRoot
}

Write-Host 'Resolving paths '
$env:PFX_PATH = resolve-path ${env:PFX_PATH}
$signtool = resolve-path $signtool
$env:SNK_PATH = resolve-path ${env:SNK_PATH}

Write-Host "Building project"
jf dotnet build `
    /nologo `
    /nr:false `
    /p:platform="Any CPU" `
    /p:configuration=$env:BUILD_CONFIGURATION `
    /p:VisualStudioVersion="17.0" `
    /p:CommitId=$env:CIRRUS_CHANGE_IN_REPO `
    /p:BranchName=$env:CIRRUS_BRANCH `
    /p:BuildNumber=$env:BUILD_NUMBER `
    /p:SignAssembly=$signAssembly `
    /p:AssemblyOriginatorKeyFile=$env:SNK_PATH `
    /p:PFX_PATH=$env:PFX_PATH `
    /p:PFX_PASSWORD=$env:SIGN_PASSPHRASE `
    /p:SIGNTOOL_PATH=$signtool `
    /p:PFX_SHA1=$env:PFX_SHA1 `
    /p:RestoreLockedMode=true `
    /p:RestoreConfigFile="nuget.Config" `
    --build-name=$buildName `
    --build-number=$buildId
CheckIfSuccessful "build"

Write-Host "Packing project"
jf dotnet pack "$env:PROJECT_DIR\\src\\SonarLint.Secrets.DotNet\\SonarLint.Secrets.DotNet.csproj" -o $env:PROJECT_DIR\artifacts -c $env:BUILD_CONFIGURATION --no-build --build-name=$buildName --build-number=$buildId
CheckIfSuccessful "packaging"

$artifactPath = resolve-path $env:PROJECT_DIR\artifacts\SonarLint.Secrets.DotNet.*.nupkg
Write-Host "Artifact path: $artifactPath"

if($signAssembly)
{
  Write-Host "Signing the $artifactPath nuget package"
  dotnet nuget sign $artifactPath --certificate-path $env:PFX_PATH --certificate-password $env:SIGN_PASSPHRASE --timestamper http://sha256timestamp.ws.symantec.com/sha256/timestamp
  CheckIfSuccessful "signing"
}

Write-Host "Publish NuGet package to artifactory"
jf rt upload $artifactPath sonarsource-nuget-qa --build-name=$buildName --build-number=$buildId
CheckIfSuccessful "Upload the NuGet package to artifactory"

Write-Host "Publish the collected build info to artifactory"
jf rt build-publish $buildName $buildId
CheckIfSuccessful "Publish the collected build info to artifactory"
