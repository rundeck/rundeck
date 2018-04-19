package rundeck.interceptors

import com.dtolabs.client.utils.Constants
import com.dtolabs.rundeck.app.api.ApiVersions


class AuthorizationInterceptor {

    int order = HIGHEST_PRECEDENCE + 50

    boolean before() {
        if (request.invalidApiAuthentication) {
            response.setStatus(403)
            def authid = session.user ?: "(${request.invalidAuthToken ?: 'unauthenticated'})"
            log.error("${authid} UNAUTHORIZED for ${controllerName}/${actionName}");
            if (request.api_version) {
                //api request
                if (response.format in ['json']) {
                    render(contentType: "application/json", encoding: "UTF-8") {
                        error = true
                        apiversion = ApiRequestFilters.API_CURRENT_VERSION
                        errorCode = "unauthorized"
                        message = ("${authid} is not authorized for: ${request.forwardURI}")
                    }
                } else {
                    render(contentType: "text/xml", encoding: "UTF-8") {
                        result(error: "true", apiversion: ApiRequestFilters.API_CURRENT_VERSION) {
                            delegate.'error'(code: "unauthorized") {
                                message("${authid} is not authorized for: ${request.forwardURI}")
                            }
                        }
                    }
                }
                return false
            }
            flash.title = "Unauthorized"
            flash.error = "${authid} is not authorized"
            response.setHeader(Constants.X_RUNDECK_ACTION_UNAUTHORIZED_HEADER, flash.error)
            redirect(controller: 'user', action: actionName ==~ /^.*(Fragment|Inline)$/ ? 'deniedFragment' : 'denied', params: params.xmlreq ? params.subMap(['xmlreq']) : null)
            return false;
        }
        return true
    }

    boolean after() { true }

    void afterView() {
        // no-op
    }
}
