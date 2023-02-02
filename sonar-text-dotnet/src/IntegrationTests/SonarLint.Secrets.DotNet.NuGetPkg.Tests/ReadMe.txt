SonarLint.Secrets.DotNet.NuGetPkg.Tests
---------------------------------------

The purpose of these tests is to do a sanity check against the final published
NuGet package i.e. the tests reference the NuGet package, rather than the product
project.

The assumption is that assembly will be:
* strong-named
* signed

Local (dev-machine) builds of the NuGet package will not normally be signed,
so these tests should normally only be run on a CI machine that is producing a publishable
artefact.
