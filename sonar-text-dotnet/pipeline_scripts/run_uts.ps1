$utsDllPath="$env:PROJECT_DIR\\src\\SonarLint.Secrets.DotNet.UnitTests\\bin\\Release\\net48\\SonarLint.Secrets.DotNet.UnitTests.dll"

Write-Host "Running UTs from path: $utsDllPath"
dotnet test  $utsDllPath  --configuration "Release"  --no-build
