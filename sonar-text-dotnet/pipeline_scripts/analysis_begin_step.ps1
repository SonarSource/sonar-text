$versionFilePath = "$env:PROJECT_DIR\Directory.Build.props"
Write-Host "Reading the Sonar project version from '${versionFilePath}' ..."

# Read the version from the file
[xml]$versionProps = Get-Content "$versionFilePath"
$sonarProjectVersion = $versionProps.Project.PropertyGroup.Version
Write-Host "Version: ${sonarProjectVersion}"

Set-Location $env:PROJECT_DIR

if ([string]::IsNullOrEmpty($env:CIRRUS_PR)) {
    Write-Host "Execute analysis begin step for master branch"

    SonarScanner.MSBuild.exe begin `
        /k:$env:PROJECT_KEY `
        /n:$env:PROJECT_NAME `
        /v:$sonarProjectVersion `
        /d:sonar.host.url=$env:SONAR_HOST_URL `
        /d:sonar.login=$env:SONAR_TOKEN `
        /d:sonar.cs.opencover.reportsPaths="**/coverage.xml" 
}
else {
    Write-Host "Execute analysis begin step on branch $env:CIRRUS_BRANCH"

    SonarScanner.MSBuild.exe begin `
        /k:$env:PROJECT_KEY `
        /n:$env:PROJECT_NAME `
        /v:$sonarProjectVersion `
        /d:sonar.host.url=$env:SONAR_HOST_URL `
        /d:sonar.login=$env:SONAR_TOKEN `
        /d:sonar.pullrequest.branch=$env:CIRRUS_BRANCH `
        /d:sonar.pullrequest.key=$env:CIRRUS_PR `
        /d:sonar.pullrequest.base=$env:CIRRUS_BASE_BRANCH `
        /d:sonar.cs.opencover.reportsPaths="**/coverage.xml" 
}

if(!$?) {
    Write-Host "Analysis begin step failed."
    Exit $LASTEXITCODE
}
