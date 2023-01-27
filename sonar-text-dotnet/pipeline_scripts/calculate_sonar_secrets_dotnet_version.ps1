$versionFilePath = "$env:CIRRUS_WORKING_DIR\sonar-text-dotnet\src\SonarLint.Secrets.DotNet\SonarLint.Secrets.DotNet.csproj"
Write-Host "Reading the Sonar project version from '${versionFilePath}' ..."
# Read the version from the file
[xml]$versionProps = Get-Content "$versionFilePath"
$sonarProjectVersion = $versionProps.Project.PropertyGroup.Version
[Environment]::SetEnvironmentVariable('PROJECT_VERSION', $sonarProjectVersion, 'Machine') # Persist this variable on machine level
Write-Host "Sonar project version is $env:PROJECT_VERSION"
