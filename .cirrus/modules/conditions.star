
# SHARED CANDIDATE
def is_main_branch():
    """
    Is the current branch the main branch?

    :return: an expression to be used in the only_if task parameter
    """
    return "$CIRRUS_USER_COLLABORATOR == 'true' && $CIRRUS_TAG == \"\" && ($CIRRUS_BRANCH == $CIRRUS_DEFAULT_BRANCH || $CIRRUS_BRANCH =~ \"branch-.*\")"
