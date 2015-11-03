package com.dtolabs.rundeck.app.api.scm

import com.dtolabs.rundeck.core.common.FrameworkResource
import grails.validation.Validateable

/**
 * Created by greg on 11/2/15.
 */
@Validateable
class ScmJobActionRequest extends ScmJobRequest implements ActionRequest {
    String actionId

    static constraints = {
        actionId(nullable: false, matches: FrameworkResource.VALID_RESOURCE_NAME_REGEX)
    }
}
