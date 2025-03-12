const fs = require('fs');
const path = require('path');
const core = require('@actions/core');
const Ajv = require('ajv');

const ajv = new Ajv();

// Load the JSON input from the environment variable
const secretsJson = process.env.SECRETS_JSON;

try {
    const parsedJson = JSON.parse(secretsJson);

    // Load the JSON schema
    const schema = {
        "type": "object",
        "properties": {
            "secrets": {
                "type": "array",
                "items": {
                    "type": "object",
                    "properties": {
                        "rspecKey": { "type": "string" },
                        "name": { "type": "string" },
                        "example": { "type": "string" },
                        "preFilter": { "type": "string" },
                        "pattern": { "type": "string" },
                        "patternAround": { "type": "string" }
                    },
                    "required": ["rspecKey", "name", "example", "preFilter", "pattern"]
                }
            }
        },
        "required": ["secrets"]
    };

    // Validate the JSON against the schema
    const validate = ajv.compile(schema);
    const valid = validate(parsedJson);

    if (!valid) {
        throw new Error(`Invalid JSON: ${ajv.errorsText(validate.errors)}`);
    }

    const secrets = parsedJson.secrets;

    // Load the template files
    const templatePath = path.join(__dirname, '../templates/githubActionSecretSpecificationSimplifiedTemplate.yaml');
    const template = fs.readFileSync(templatePath, 'utf8');
    const templatePathWithPatternAround = path.join(__dirname, '../templates/githubActionSecretSpecificationSimplifiedTemplateWithPatternAround.yaml');
    const templateWithPatternAround = fs.readFileSync(templatePathWithPatternAround, 'utf8');

    const slugify = (str) => str.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/(^-|-$)+/g, '');
    const toConstantCase = (str) => str.toUpperCase().replace(/[^A-Z0-9]+/g, '_').replace(/(^_|_$)+/g, '');
    const escapeSpecialCharacters = (str) => str.replace(/\\/g, '\\\\').replace(/"/g, '\\"').replace(/\n/g, '\\n');

    // Generate files for each secret
    secrets.forEach(secret => {
        const { rspecKey, name, example, preFilter, pattern, patternAround } = secret;
        const nameSlugified = slugify(name);
        const nameConstantCase = toConstantCase(nameSlugified);

        // Replace placeholders in the template
        const usedTemplate = patternAround ? templateWithPatternAround : template;
        const content = usedTemplate
                .replace(/\$\{RSPEC_KEY\}/g, rspecKey)
                .replace(/\$\{NAME\}/g, name)
                .replace(/\$\{NAME_SLUGIFIED\}/g, nameSlugified)
                .replace(/\$\{NAME_CONSTANT\}/g, nameConstantCase)
                .replace(/\$\{EXAMPLE\}/g, escapeSpecialCharacters(example))
                .replace(/\$\{PRE_FILTER\}/g, escapeSpecialCharacters(preFilter))
                .replace(/\$\{PATTERN\}/g, escapeSpecialCharacters(pattern))
                .replace(/\$\{PATTERN_AROUND\}/g, patternAround ? escapeSpecialCharacters(patternAround) : '');

        // Write the content to a new file
        const outputPath = path.join(__dirname, `../../../../../private/sonar-text-developer-plugin/src/main/resources/com/sonar/plugins/secrets/configuration/${nameSlugified}.yaml`);
        fs.writeFileSync(outputPath, content, 'utf8');
        console.log(`Generated file: ${outputPath}`);

        // Write the generic RSPEC HTML metadata
        const genericHtmlRspecPath = path.join(__dirname, `./generic_rspec.html`);
        const outputHtmlRspecPath = path.join(__dirname, `../../../../../private/sonar-text-developer-plugin/src/main/resources/com/sonar/l10n/secrets/rules/secrets/${rspecKey}.html`);
        fs.copyFileSync(genericHtmlRspecPath, outputHtmlRspecPath);

        // Write the generic RSPEC JSON metadata
        const genericJsonRspecTemplatePath = path.join(__dirname, `./generic_rspec.json`);
        const genericJsonRspecTemplate = fs.readFileSync(genericJsonRspecTemplatePath, 'utf8');
        const rspecId = rspecKey.replace('S', '');
        const genericJsonRspec = genericJsonRspecTemplate.replace(/\$\{RSPEC_ID\}/g, rspecId);
        const outputJsonRspecPath = path.join(__dirname, `../../../../../private/sonar-text-developer-plugin/src/main/resources/com/sonar/l10n/secrets/rules/secrets/${rspecKey}.json`);
        fs.writeFileSync(outputJsonRspecPath, genericJsonRspec, 'utf8');
    });

    // Set the output for the GitHub Action
    core.exportVariable('RSPEC_KEY', secrets[0].rspecKey);
    core.exportVariable('RSPEC_KEYS', secrets.map(secret => secret.rspecKey).join(', '));

    // Add the new RSPEC keys to the Sonar-way profile
    const sonarWayProfilePath = path.join(__dirname, `../../../../../private/sonar-text-developer-plugin/src/main/resources/com/sonar/l10n/secrets/rules/secrets/Sonar_way_profile.json`);
    const sonarWayProfile = fs.readFileSync(sonarWayProfilePath, 'utf8');
    const sonarWayProfileJson = JSON.parse(sonarWayProfile);
    const rspecKeys = secrets.map(secret => secret.rspecKey);
    sonarWayProfileJson.ruleKeys = sonarWayProfileJson.ruleKeys.concat(rspecKeys);
    fs.writeFileSync(sonarWayProfilePath, JSON.stringify(sonarWayProfileJson, null, 2)  + '\n', 'utf8');
} catch (error) {
    console.error('Error processing secrets JSON:', error.message);
    core.setFailed(error.message);
    process.exit(1);
}
