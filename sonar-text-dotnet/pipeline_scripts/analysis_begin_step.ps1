cd $env:PROJECT_DIR

Write-Host "Execute analysis begin step"

  SonarScanner.MSBuild.exe begin `
  /n:$env:PROJECT_NAME `
  /v:$env:CIRRUS_CHANGE_IN_REPO `
  /d:sonar.security.hardfail=true `
  /d:sonar.host.url=$env:SONAR_HOST_URL `
  /d:sonar.login=$env:SONAR_TOKEN `
  /d:sonar.xml.file.suffixes=.no_xml_files `
  /d:sonar.java.file.suffixes=.no_java_files `
  /d:sonar.scm.disabled=true `
  /d:sonar.branch.autoconfig.disabled=true `
  /d:sonar.cpd.exclusions=** `
  /d:sonar.security.monitoring=true `
  /d:sonar.security.truncateLargeFlows=false