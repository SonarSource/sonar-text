Set-Location $env:PROJECT_DIR

if ([string]::IsNullOrEmpty($env:CIRRUS_PR)) {
    Write-Host "Execute analysis begin step for master branch"

    SonarScanner.MSBuild.exe begin `
        /k:$env:PROJECT_NAME `
        /n:$env:PROJECT_NAME `
        /v:$env:PROJECT_VERSION `
        /d:sonar.host.url=$env:SONAR_HOST_URL `
        /d:sonar.login=$env:SONAR_TOKEN `
}
else {
    Write-Host "Execute analysis begin step on branch $env:CIRRUS_BRANCH"

    SonarScanner.MSBuild.exe begin `
        /k:$env:PROJECT_NAME `
        /n:$env:PROJECT_NAME `
        /d:sonar.host.url=$env:SONAR_HOST_URL `
        /d:sonar.login=$env:SONAR_TOKEN `
        /d:sonar.pullrequest.branch=$env:CIRRUS_BRANCH `
        /d:sonar.pullrequest.key=$env:CIRRUS_PR `
        /d:sonar.pullrequest.base=$env:CIRRUS_BASE_BRANCH
}

if(!$?) {
    Write-Host "Analysis begin step failed."
    Exit $LASTEXITCODE
}
