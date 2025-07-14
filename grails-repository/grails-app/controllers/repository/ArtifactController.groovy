package repository

import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.core.authorization.AuthContextProcessor
import grails.converters.JSON
import groovy.transform.PackageScope
import org.rundeck.core.auth.AuthConstants

class ArtifactController {

    AuthContextProcessor rundeckAuthContextProcessor


    def index() {
        if(!authorized(AuthConstants.RESOURCE_TYPE_PLUGIN, AuthConstants.ACTION_READ)) {
            specifyUnauthorizedError()
            return
        }
        render(view: "index")
    }


    @PackageScope
    boolean authorized(Map resourceType = AuthConstants.RESOURCE_TYPE_SYSTEM, String action = AuthConstants.ACTION_ADMIN) {
        List authorizedActions = [AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_OPS_ADMIN]
        if(action != AuthConstants.ACTION_ADMIN) authorizedActions.add(action)
        AuthContext authContext = rundeckAuthContextProcessor.getAuthContextForSubject(session.subject)
        rundeckAuthContextProcessor.authorizeApplicationResourceAny(authContext, resourceType, authorizedActions)
    }

    private def specifyUnauthorizedError() {
        response.setStatus(400)
        def err = [error: "You are not authorized to perform this action"]
        render err as JSON
    }
}
