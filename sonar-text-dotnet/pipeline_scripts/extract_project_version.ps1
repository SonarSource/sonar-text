$versionFilePath = "$env:CIRRUS_WORKING_DIR\sonar-text-dotnet\Directory.Build.props"
Write-Host "Reading the Sonar project version from '${versionFilePath}' ..."

# Read the version from the file
[xml]$versionProps = Get-Content "$versionFilePath"
$sonarProjectVersion = $versionProps.Project.PropertyGroup.Version

# Set persisten env variable PROJECT_VERSION
[Environment]::SetEnvironmentVariable('PROJECT_VERSION', $sonarProjectVersion, 'Machine') # Persist this variable on machine level

Write-Host "Version is env:$PROJECT_VERSION"