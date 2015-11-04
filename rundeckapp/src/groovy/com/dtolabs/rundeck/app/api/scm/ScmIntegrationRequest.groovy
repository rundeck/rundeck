package com.dtolabs.rundeck.app.api.scm

import com.dtolabs.rundeck.core.common.FrameworkResource
import grails.validation.Validateable

/**
 * Created by greg on 10/27/15.
 */
@Validateable
class ScmIntegrationRequest implements ProjectRequest,IntegrationRequest{
    String project
    String integration

    static constraints={
        project(nullable:false, matches: FrameworkResource.VALID_RESOURCE_NAME_REGEX)
        integration(nullable: false, inList: ['export', 'import'])
    }
}
