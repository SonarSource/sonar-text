There are Azure DevOps extensions that download, install and run the 
obfuscation tool. See the following two extensions:

* https://marketplace.visualstudio.com/items?itemName=Eziriz.reactor-tool-task
* https://marketplace.visualstudio.com/items?itemName=Eziriz.azure-reactor-automation-task

However, we're installing the tool from our own Azure DevOps Universal package and 
then executing it from our MSBuild project. This means we don't need to pull assets from
external sources when building.

This does come with some downsides:
* we need to do more work to update the version of the obfuscation tool.
* we need to duplicate some of the things the tasks do to make sure the tool
  recognises and uses the license file.
  
  Fortunately, this is currently quite straightforward (although it could change
  in newer versions of the tool):
  * drop all of the tool binary files and the license file in the same folder
  * set the environment variable "DOTNETREACTORROOT" to point to this folder
  
  This is done in the yaml file.



HOW-TO: Update the Azure DevOps Universal Package with a new .NET Reactor version
---------------------------------------------------------------------------------

1) Download the new version from Eziriz
   e.g. https://www.eziriz.com/downloads/reactor_devops_6_8_0_0.zip

2) Unpack the zip into a new directory

3) Push everything in the directory to Azure DevOps, specifying a new version number.



HOW-TO: Push a new version of the package to Azure DevOps
---------------------------------------------------------
A universal package can be published from the command line once the _az_ CLI tool and the _az_ devops extension have been installed.

Command line ref: https://docs.microsoft.com/en-us/cli/azure/artifacts/universal?view=azure-cli-latest

Step (1): PAT token
Make sure you have a PAT token with permission to publish to the package feed.

Step (2): login
az devops login --organization https://dev.azure.com/sonarsource/

Step (3): publish (everything in the current directory)

  Don't forget to update the package version.

e.g. 
az artifacts universal publish --organization "https://dev.azure.com/sonarsource/" --project "399fb241-ecc7-4802-8697-dcdd01fbb832" --scope project --feed "slvs_obfusc_proto" --name "dotnetreactor" --version "0.0.2" --path .




