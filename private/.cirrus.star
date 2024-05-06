# Check Starlark specs: https://github.com/bazelbuild/starlark/blob/master/spec.md

load("../.cirrus/modules/helper.star", "merge_dict")
load("../.cirrus/modules/env.star", "env")
load("../.cirrus/modules/build.star", "build_task", "sca_scan_task")
load(
    "../.cirrus/modules/qa.star",
    "qa_plugin_task",
    "qa_ruling_task",
    "qa_benchmark_task"
)
load("../.cirrus/modules/promote.star", "promote_task")


def private_pipeline_builder():
    conf = dict()
    merge_dict(conf, env())
    merge_dict(conf, build_task())
    merge_dict(conf, sca_scan_task())
    merge_dict(conf, qa_plugin_task())
    merge_dict(conf, qa_ruling_task())
    merge_dict(conf, qa_benchmark_task())
    merge_dict(conf, promote_task())
    return conf
