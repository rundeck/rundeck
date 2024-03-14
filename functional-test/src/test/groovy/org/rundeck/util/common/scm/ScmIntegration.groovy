package org.rundeck.util.common.scm

enum ScmIntegration {
    IMPORT("import"),
    EXPORT("export"),
    INVALID("asddsa")
    final String name
    ScmIntegration(String name){
        this.name = name
    }

    static ScmIntegration getEnum(String value) {
        for(ScmIntegration v : values())
            if(v.name == value) return v
        throw new IllegalArgumentException()
    }
}