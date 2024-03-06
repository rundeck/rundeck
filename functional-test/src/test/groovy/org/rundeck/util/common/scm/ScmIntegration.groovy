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
        ScmIntegration action = valueOf(value.toUpperCase(Locale.ENGLISH))
        if(action.name == value) return action
        throw new IllegalArgumentException()
    }
}