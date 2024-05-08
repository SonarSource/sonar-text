load("github.com/SonarSource/cirrus-modules@v2", "load_features")
load("cirrus", "env", "fs")
load(".cirrus/modules/helper.star", "merge_dict")
load("private/.cirrus.star", "private_pipeline_builder")


# workaround for BUILD-4413 (build number on public CI)
def build_4413_workaround():
    return {
        'env': {
            'CI_BUILD_NUMBER': env.get("CIRRUS_PR", "1")
        },
    }


def main(ctx):
    # Manage the case of the private repository
    if env.get("CIRRUS_REPO_FULL_NAME") == 'SonarSource/sonar-text-enterprise':
        features = load_features(ctx, only_if=dict())
        doc = private_pipeline_builder()
    # Manage the case of the public repository
    else:
        doc = fs.read(".cirrus-public.yml")
        if env.get("CIRRUS_USER_PERMISSION") in ["write", "admin"]:
            features = load_features(ctx, features=["build_number"])
        else:
            features = build_4413_workaround()

    conf = dict()
    merge_dict(conf, features)
    merge_dict(conf, doc)
    return conf
