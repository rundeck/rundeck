package org.rundeck.util.common.scm

enum ScmActionId {
    JOB_COMMIT("job-commit"),
    JOB_IMPORT("import-jobs"),
    PROJECT_COMMIT("project-commit")
    final String name
    ScmActionId(String name){
        this.name = name
    }

    static ScmActionId getEnum(String value) {
        for(ScmActionId v : values())
            if(v.name == value) return v
        throw new IllegalArgumentException()
    }
}