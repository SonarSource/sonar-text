## Verification of the code generation logic
1. Ensure that the unit test is passing:
    ```bash
    ./gradlew :build-logic:test
    ```
2. Create a placeholder specification:
   ```bash
   cd ..
   cat <<EOF > sonar-text-plugin/src/main/resources/org/sonar/plugins/secrets/configuration/foobar.yaml
   provider:
     metadata:
       name: Foobar Detection
     rules:
       - rspecKey: S8888
         id: foobar
   EOF
   ```
3. Run the code generation logic:
   ```bash
   ./secretSpecificationInclusionGenerator.sh
   ```
   This should fail when running rule-api, because the rule key `S8888` does not exist.
4. Verify that the generated code is correct:
   * Files FoobarCheck.java and FoobarCheckTest.java should exist in the sonar-text-plugin module.

Alternatively, to test generation, remove files for an existing check: `<CheckName>Check.java` and `<CheckName>CheckTest.java`, as well as `<rspecKey>.json` and `<rspecKey>.html`. After executing the script, these files should be back. To test cleanup of unused classes, remove a YAML specification and run the script. The four files mentioned previously should be removed.
