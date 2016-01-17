package com.dtolabs.rundeck.app.api.scm

import com.dtolabs.rundeck.core.common.FrameworkResource
import grails.validation.Validateable

/**
 * Job SCM request
 */
@Validateable
class ScmJobRequest {
    String id
    String integration

    static constraints = {
        id(nullable: false, matches: FrameworkResource.VALID_RESOURCE_NAME_REGEX)
        integration(nullable: false, inList: ['export', 'import'])
    }
}
