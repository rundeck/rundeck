package com.dtolabs.rundeck.app.api.scm

import grails.validation.Validateable
import groovy.transform.CompileStatic

class ScmToggleRequest implements  Validateable {
    Boolean enabled
    static constraints = {
        enabled(nullable: false)
    }
}
