cd $env:PROJECT_DIR
Write-Host Restoring $env:SOLUTION_DIR
nuget restore -LockedMode -Source https://pkgs.dev.azure.com/sonarsource/399fb241-ecc7-4802-8697-dcdd01fbb832/_packaging/slvs_input/nuget/v3/index.json  # nuget version is 6.3.1 https://github.com/SonarSource/re-ci-images/blob/master/ec2-images/build-base-windows-dotnet.pkr.hcl#L39
Write-Host Building $env:SOLUTION_DIR
dotnet build --no-restore /nologo /nr:false //p:platform="Any CPU" /p:configuration="Release" /p:VisualStudioVersion="17.0"