# Calculate the file path
$BuildConfiguration="Release"
$Major="0"
$Minor="0"
$versionFilePath = "$env:CIRRUS_WORKING_DIR\sonar-text-dotnet\src\SonarLint.Secrets.DotNet\SonarLint.Secrets.DotNet.csproj"
Write-Host "Reading the Sonar project version from '${versionFilePath}' ..."
# Read the version from the file
[xml]$versionProps = Get-Content "$versionFilePath"
$sonarProjectVersion = $versionProps.Project.PropertyGroup.AssemblyVersion

Write-host $version
Write-Host "Sonar project version is '${sonarProjectVersion}'"
# Set the variable to it can be used by other tasks
Write-Host $sonarProjectVersion
