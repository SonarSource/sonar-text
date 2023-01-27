$buildConfiguration="Release"

cd $env:PROJECT_DIR
Write-Host Restoring $env:SOLUTION_DIR
nuget restore -LockedMode -Source "https://pkgs.dev.azure.com/sonarsource/399fb241-ecc7-4802-8697-dcdd01fbb832/_packaging/slvs_input/nuget/v3/index.json"  # nuget version is 6.3.1 https://github.com/SonarSource/re-ci-images/blob/master/ec2-images/build-base-windows-dotnet.pkr.hcl#L39

$signArtifacts=$false
if(($env:CIRRUS_BRANCH -eq "master" ) -or ($env:CIRRUS_BRANCH.StartsWith("branch-"))
{
    $signArtifacts=true
    Write-Host "signArtifacts variable was set to {$signArtifacts}".
}

Write-Host Building $env:SOLUTION_DIR
dotnet build `
    --no-restore `
     /nologo `
     /nr:false `
     /p:platform="Any CPU" `
     /p:configuration=$buildConfiguration `
     /p:VisualStudioVersion="17.0" `
     /p:SignAssembly=$signArtifacts `
     /p:CommitId=$env:CIRRUS_CHANGE_IN_REPO `
     /p:BranchName=$env:CIRRUS_BRANCH `
     /p:BuildNumber=$BUILD_NUMBER

