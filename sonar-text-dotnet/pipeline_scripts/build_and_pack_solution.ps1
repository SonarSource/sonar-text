function CheckIfSuccessful($StepName) {
  if(!$?) {
    Write-Host "sonar-text-dotnet ${StepName} failed."
    Exit $LASTEXITCODE
  }
}

Set-Location $env:PROJECT_DIR
$SIGN_DOTNET_ASSEMBLY = "$CIRRUS_BRANCH" -eq "master" -or "$CIRRUS_BRANCH".startsWith("branch-.*")
Write-Host "Should the assembly be signed: ${SIGN_DOTNET_ASSEMBLY}"

Write-Host Restoring $env:SOLUTION_DIR
$nugetFeed = "https://pkgs.dev.azure.com/sonarsource/399fb241-ecc7-4802-8697-dcdd01fbb832/_packaging/slvs_input/nuget/v3/index.json"
# For nuget version see: https://github.com/SonarSource/re-ci-images/blob/master/ec2-images/build-base-windows-dotnet.pkr.hcl#L39
nuget restore -LockedMode -Source $nugetFeed
CheckIfSuccessful "nuget restore"

Write-Host Building $env:SOLUTION_DIR
dotnet build `
    $env:SOLUTION_DIR `
    --no-restore `
    /nologo `
    /nr:false `
    /p:platform="Any CPU" `
    /p:configuration=$env:BUILD_CONFIGURATION `
    /p:VisualStudioVersion="17.0" `
    /p:CommitId=$env:CIRRUS_CHANGE_IN_REPO `
    /p:BranchName=$env:CIRRUS_BRANCH `
    /p:BuildNumber=$env:BUILD_NUMBER `
    /p:SignAssembly=$SIGN_DOTNET_ASSEMBLY `
    /p:AssemblyOriginatorKeyFile=$env:SNK_PATH
CheckIfSuccessful "build"

Write-Host Packing $env:SOLUTION_DIR
dotnet pack $env:SOLUTION_DIR -o $env:PROJECT_DIR\artifacts -c $env:BUILD_CONFIGURATION --no-build
CheckIfSuccessful "packaging"

if($SIGN_DOTNET_ASSEMBLY) {
  $ARTIFACT_PATH = resolve-path ${env:PROJECT_DIR}\artifacts\SonarLint.Secrets.DotNet.*.nupkg
  Write-Host "Signing the ${ARTIFACT_PATH} nuget package"
  nuget sign $ARTIFACT_PATH -CertificatePath $env:PFX_PATH -CertificatePassword $env:SIGN_PASSPHRASE -Timestamper http://sha256timestamp.ws.symantec.com/sha256/timestamp -NonInteractive
  CheckIfSuccessful "signing"
}