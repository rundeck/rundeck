package com.rundeck.plugin

import grails.validation.Validateable

/**
 * Command object for execution mode later API endpoints
 * Grails 7 / Spring Boot 3: Modern approach using @RequestBody with validation
 */
class ExecutionModeLaterCommand implements Validateable {
    String value
    
    static constraints = {
        value nullable: false, blank: false, validator: { val, obj ->
            if (!val) {
                return 'required'
            }
            if (!PluginUtil.validateTimeDuration(val)) {
                return 'invalid.time.format'
            }
        }
    }
}

