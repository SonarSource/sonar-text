function Get-Version {
  $versionFilePath = Join-Path $env:PROJECT_DIR "Directory.Build.props"
  Write-Host "Reading the Sonar project version from '${versionFilePath}' ..."
  # Read the version from the file
  [xml]$versionProps = Get-Content "$versionFilePath"
  return $versionProps.Project.PropertyGroup.Version[1] # Versions is an array of objects in powershell
}

if ("$env:CIRRUS_BRANCH" -ne "master" -and (-not ("$env:CIRRUS_BRANCH".startsWith("branch-"))))
{
  Exit 0
}

Set-Location $env:PROJECT_DIR
Write-host "Create tools directory"
$toolsPath = "C:\tools"
if (-Not [System.IO.Directory]::Exists($toolsPath)){
  New-Item -Path "C:\" -Name "tools" -ItemType "directory"
}

$NUM_RETRIES = 5
for ($num = 1 ; $num -le $NUM_RETRIES ; $num++)
{
  try
  {
    Write-host "Download Mend tool, attempt $num/$NUM_RETRIES"
    $MendAgentPath = Join-Path $toolsPath "wss-unified-agent.jar"
    Invoke-WebRequest -Uri https://unified-agent.s3.amazonaws.com/wss-unified-agent.jar -OutFile $MendAgentPath
    break
  }
  catch
  {
    if ([System.IO.File]::Exists($MendAgentPath))
    {
      Remove-Item -Path $MendAgentPath
    }
    Write-host "Download failed with error: $_"

    if($num -lt $NUM_RETRIES)
    {
      Write-host "Will wait 5s before retry."
      Start-Sleep -Seconds 5
    }
  }
}

Write-Host "Validating Mend agent jar signature..."
& "$env:JAVA_HOME\bin\jarsigner.exe" -verify -strict -verbose $MendAgentPath
if (-Not $?) # if result is "jar is unsigned" exit code is false, otherwise it's true.
{
  Write-Host "wss-unified-agent.jar signature verification failed."
  exit 1
}

Write-Host "Download wss-unified-agent.jar.sha256 file"
$shaPath = Join-Path $toolsPath "wss-unified-agent.jar.sha256"
Invoke-WebRequest -Uri https://unified-agent.s3.amazonaws.com/wss-unified-agent.jar.sha256 -OutFile $shaPath
if (-Not (Test-Path -Path $shaPath)){
  Write-Host "wss-unified-agent.jar.sha256 file does not exist - cannot complete signature verification."
  exit 1
}

Write-Host "Validating Mend agent jar hash..."
if (-Not (Get-Content $shaPath).split(" ")[0] -eq (Get-FileHash $MendAgentPath).Hash)
{
    Write-Host "Failed to verify jar hash".
    exit 1
}

# Mend agent needs the following environment variables:
# - WS_APIKEY
# - WS_PRODUCTNAME
# - WS_PROJECTNAME

$env:WS_PROJECTNAME = "SonarSource/$env:PROJECT_NAME $(Get-Version)"

Write-Host "Running the Mend unified agent for $env:WS_PROJECTNAME..."
& "$env:JAVA_HOME\bin\java.exe" -jar $MendAgentPath -c "$PSScriptRoot\wss-unified-agent.config"
