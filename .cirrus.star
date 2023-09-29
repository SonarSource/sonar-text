load("github.com/SonarSource/cirrus-modules@v2", "load_features")
load("cirrus", "env", "fs", "yaml")


def main(ctx):
    doc = fs.read("private/.cirrus.yml") if env.get("CIRRUS_REPO_FULL_NAME") == 'SonarSource/sonar-text-enterprise' \
        else fs.read(".cirrus-public.yml") or ''
    features = yaml.dumps(load_features(ctx, only_if=dict()))
    return features + doc
