package org.rundeck.util.common.scm

enum ScmActionId {
    JOB_COMMIT("job-commit"),
    PROJECT_COMMIT("project-commit")
    final String name
    ScmActionId(String name){
        this.name = name
    }

    static ScmActionId getEnum(String value) {
        ScmActionId action = valueOf(value.toUpperCase(Locale.ENGLISH))
        if(action.name == value) return action
        throw new IllegalArgumentException()
    }
}