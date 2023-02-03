Set-Location $env:PROJECT_DIR

Write-Host Restoring $env:SOLUTION_DIR
$nugetFeed = "https://pkgs.dev.azure.com/sonarsource/399fb241-ecc7-4802-8697-dcdd01fbb832/_packaging/slvs_input/nuget/v3/index.json"
# For nuget version see: https://github.com/SonarSource/re-ci-images/blob/master/ec2-images/build-base-windows-dotnet.pkr.hcl#L39
nuget restore -LockedMode -Source $nugetFeed

if(!$?) {
    Write-Host "Nuget restore failed."
    Exit $LASTEXITCODE
}

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
    /p:BuildNumber=$env:BUILD_NUMBER

if(!$?) {
    Write-Host "sonar-text-dotnet build failed."
    Exit $LASTEXITCODE
}
