
def merge_dict(target, source):
    for key in source.keys():
        if target.get(key) == None:
            target.update({key: source[key]})
        else:
            target[key].update(source[key])
