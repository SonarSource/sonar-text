Set-Location $env:PROJECT_DIR

Write-Host "Execute analysis begin step"

SonarScanner.MSBuild.exe begin `
	/k:$env:PROJECT_NAME `
	/n:$env:PROJECT_NAME `
	/v:$env:CIRRUS_CHANGE_IN_REPO `
	/d:sonar.host.url=$env:SONAR_HOST_URL `
	/d:sonar.login=$env:SONAR_TOKEN `
	/d:sonar.pullrequest.branch=$env:CIRRUS_BRANCH `
	/d:sonar.pullrequest.key=$env:CIRRUS_PR `
	/d:sonar.pullrequest.base=$env:CIRRUS_BASE_BRANCH

