package com.rundeck.plugin

import grails.validation.Validateable

/**
 * Command object for project execution mode later API endpoints
 * Grails 7 / Spring Boot 3: Modern approach using @RequestBody with validation
 */
class ProjectExecutionModeLaterCommand implements Validateable {
    String type  // 'executions' or 'schedule'
    String value
    
    static constraints = {
        type nullable: false, blank: false, inList: ['executions', 'schedule']
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

