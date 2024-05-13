# SHARED
def merge_dict(target, source):
    """
    Merge two dictionaries.

    If the key is not in the target, add it.
    If the key is in the target, merge the values.

    :param target: The dictionary that is updated from the source
    :param source: The dictionary to pull the values from
    :return: Nothing
    """
    for key in source.keys():
        if target.get(key) == None:
            target.update({key: source[key]})
        else:
            target[key].update(source[key])
