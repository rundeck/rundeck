
import org.apache.log4j.Logger
import org.apache.log4j.MDC
import org.codehaus.groovy.grails.web.util.WebUtils

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/*
 * Copyright 2011 DTO Labs, Inc. (http://dtolabs.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

/*
 * ApiRequestFilters.java
 * 
 * User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * Created: Feb 1, 2011 12:14:56 PM
 * 
 */

public class ApiRequestFilters {
    static final Logger logger = Logger.getLogger('org.rundeck.api.requests')
    public static final int V1 = 1
    public static final int V2 = 2
    public static final int V3 = 3
    public static final int V4 = 4
    public static final int V5 = 5
    public static final Map VersionMap = [:]
    public static final List Versions = [V1, V2, V3, V4, V5]
    static {
        Versions.each { VersionMap[it.toString()] = it }
    }
    public static final Set VersionStrings = new HashSet(VersionMap.values())

    public final static int API_EARLIEST_VERSION = V1
    public final static int API_CURRENT_VERSION = V5
    public final static int API_MIN_VERSION = API_EARLIEST_VERSION
    public final static int API_MAX_VERSION = API_CURRENT_VERSION

    static def logDetail(HttpServletRequest request,  params, String action, String controller, String message = null) {
        Map context = [
                remoteHost: request.remoteHost,
                version: request.api_version ?: '?',
                remoteUser:request.remoteUser?: request.authenticatedUser,
                valid:!(request.invalidApiAuthentication),
                authToken:(request.authenticatedToken?(request.authenticatedToken?.size() > 5 ? request.authenticatedToken.substring(0, 5) :'') + "****":'-'),
                controller: controller,
                action: action,
                params: params,
                uri: request.getAttribute(WebUtils.FORWARD_REQUEST_URI_ATTRIBUTE)?: request.getRequestURI(),
                userAgent:request.getHeader('User-Agent')?:'-',
                method:request.method,
                secure:Boolean.toString(request.isSecure())
        ]
        MDC.clear()
        context.each {MDC.put(it.key,it.value?:'')}
        try {
            logger.info(message ?: context.toString())
        } finally {
            MDC.clear()
        }
    }

    def allowed_actions = ["renderError", "invalid", "error"]
    def filters = {
            /**
             * Require valid api version in request path /api/version/...
             */
            apiVersion(uri:'/api/**') {
                before = {
                    if(controllerName=='api' && allowed_actions.contains(actionName) || request.api_version){
                        return true
                    }

                if (!params.api_version) {
                    flash.errorCode = 'api.error.api-version.required'
                    redirect(controller: 'api', action: 'renderError')
                    logDetail(request, params.toString(), actionName, controllerName, 'api.error.api-version.required')
                    return false
                }
                def unsupported = !(VersionMap.containsKey(params.api_version))
                if (unsupported) {
                    render(contentType: "text/xml", encoding: "UTF-8") {
                        result(error: "true", apiversion: API_CURRENT_VERSION) {
                            delegate.'error' {
                                message("Unsupported API Version \"${params.api_version}\". API Request: ${request.forwardURI}. Reason: Current version: ${API_CURRENT_VERSION}")
                            }
                        }
                    }
                    logDetail(request, params.toString(), actionName, controllerName, 'api.error.api-version.unsupported')
                    return false;
                }
                request.api_version = VersionMap[params.api_version]
                request['ApiRequestFilters.request.parameters']=params.toString()
                return true
            }
            after = {
                logDetail(request, request['ApiRequestFilters.request.parameters'], actionName, controllerName)
            }
        }
    }
}
