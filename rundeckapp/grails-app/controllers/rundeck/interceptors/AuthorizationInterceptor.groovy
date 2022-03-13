package rundeck.interceptors

import com.dtolabs.client.utils.Constants
import com.dtolabs.rundeck.app.api.ApiVersions
import org.rundeck.app.access.InterceptorHelper

import javax.servlet.http.HttpServletResponse

class AuthorizationInterceptor {

    int order = HIGHEST_PRECEDENCE + 50

    InterceptorHelper interceptorHelper

    AuthorizationInterceptor() {
        matchAll()
    }

    boolean before() {
        if(interceptorHelper.matchesAllowedAsset(controllerName, request)) return true
        if(request.apiVersionStatusNotReady){
            response.status = HttpServletResponse.SC_SERVICE_UNAVAILABLE
            return false
        }else if (request.invalidApiAuthentication) {
            response.setStatus(403)
            def authid = session.user ?: "(${request.invalidAuthToken ?: 'unauthenticated'})"
            log.error("${authid} UNAUTHORIZED for ${controllerName}/${actionName}");
            //api request
            if (response.format in ['xml']) {
                render(contentType: "text/xml", encoding: "UTF-8") {
                    result(error: "true", apiversion: ApiVersions.API_CURRENT_VERSION) {
                        delegate.'error'(code: "unauthorized") {
                            message("${authid} is not authorized for: ${request.forwardURI}")
                        }
                    }
                }
            } else {
                render(contentType: "application/json", encoding: "UTF-8") {
                    error true
                    apiversion ApiVersions.API_CURRENT_VERSION
                    errorCode "unauthorized"
                    message ("${authid} is not authorized for: ${request.forwardURI}")
                }
            }
            return false
        }
        return true
    }

    boolean after() { true }

    void afterView() {
        // no-op
    }
}
