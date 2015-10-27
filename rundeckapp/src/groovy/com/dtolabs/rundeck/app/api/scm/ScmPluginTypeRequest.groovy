package com.dtolabs.rundeck.app.api.scm

import com.dtolabs.rundeck.core.common.FrameworkResource
import grails.validation.Validateable

/**
 * Created by greg on 10/27/15.
 */
@Validateable
class ScmPluginTypeRequest extends ScmIntegrationRequest {
    String type

    static constraints={
        type(nullable:false, matches: FrameworkResource.VALID_RESOURCE_NAME_REGEX)
    }
}
