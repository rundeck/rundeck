package rundeck.filters

import com.codahale.metrics.MetricRegistry
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
    private static final String METRIC_TIMER = 'ApiRequestFilters._METRIC_TIMER'
    private static final String REQUEST_TIME = 'ApiRequestFilters._TIMER'

    def dependsOn = [AA_TimerFilters]

    def MetricRegistry metricRegistry
    def messageSource
    def apiService
    public static final int V1 = 1
    public static final int V2 = 2
    public static final int V3 = 3
    public static final int V4 = 4
    public static final int V5 = 5
    public static final int V6 = 6
    public static final int V7 = 7
    public static final int V8 = 8
    public static final int V9 = 9
    public static final int V10 = 10
    public static final int V11 = 11
    public static final int V12 = 12
    public static final int V13 = 13
    public static final int V14 = 14
    public static final int V15 = 15
    public static final int V16 = 16
    public static final int V17 = 17
    public static final Map VersionMap = [:]
    public static final List Versions = [V1, V2, V3, V4, V5, V6, V7, V8, V9, V10, V11, V12, V13, V14,V15,V16,V17]
    static {
        Versions.each { VersionMap[it.toString()] = it }
    }
    public static final Set VersionStrings = new HashSet(VersionMap.values())

    public final static int API_EARLIEST_VERSION = V1
    public final static int API_CURRENT_VERSION = V17
    public final static int API_MIN_VERSION = API_EARLIEST_VERSION
    public final static int API_MAX_VERSION = API_CURRENT_VERSION

    static def logDetail(HttpServletRequest request,  project, String action, String controller, String message = null) {
        request[METRIC_TIMER].stop()
        
        Map context = [
                remoteHost: request.remoteHost,
                version: request.api_version ?: '?',
                remoteUser: request.remoteUser ?: request.authenticatedUser,
                valid: !(request.invalidApiAuthentication),
                authToken: (request.authenticatedToken ? 'token' : 'form'),
                controller: controller,
                action: action,
                uri: request.getAttribute(WebUtils.FORWARD_REQUEST_URI_ATTRIBUTE) ?: request.getRequestURI(),
                userAgent: request.getHeader('User-Agent') ?: '-',
                method: request.method,
                secure: request.isSecure() ? 'https' : 'http',
                duration: System.currentTimeMillis() - request[REQUEST_TIME],
                project: project
        ]
        MDC.clear()
        context.each { MDC.put(it.key, it.value ?: '') }
        try {
            logger.info(message ? message + context : context.toString())
        } finally {
            MDC.clear()
        }
    }

    def allowed_actions = ["renderError", "invalid", "error"]
    def allowed_pre_api_reqs=[
            'user':['login','loggedout'],
            'menu':['index','home'],
    ]
    def filters = {
        /**
         * Disallow api access if a request comes for non-api url after login
         */
        apiAccess(uri: '/api/**', invert: true) {
            before = {
                if(allowed_pre_api_reqs[controllerName] && (actionName in allowed_pre_api_reqs[controllerName])){
                    return true
                }
                if (null == session.api_access_allowed) {
                    log.debug("Disallowing API access, blocked due to request for ${controllerName}/${actionName}")
                    session.api_access_allowed = false
                }
                true
            }
        }
        /**
         * Require valid api version in request path /api/version/...
         */
        apiVersion(uri: '/api/**') {
            before = {
                request[REQUEST_TIME]=System.currentTimeMillis()
                request[METRIC_TIMER]= timer()
                if (request.remoteUser && null != session.api_access_allowed && !session.api_access_allowed) {
                    log.debug("Api access request disallowed for ${request.forwardURI}")
                    response.sendError(HttpServletResponse.SC_NOT_FOUND)
                    return false
                }else if(null==session.api_access_allowed){
                    session.api_access_allowed=true
                }
                if (controllerName == 'api' && allowed_actions.contains(actionName) || request.api_version) {
                    request.is_allowed_api_request = true
                    return true
                }

                if (!params.api_version) {
                    flash.errorCode = 'api.error.api-version.required'
                    AA_TimerFilters.afterRequest(request, response, session)
                    logDetail(request, params.toString(), actionName, controllerName, 'api.error.api-version.required')
                    apiService.renderErrorFormat(response,[code: 'api.error.api-version.required'])
                    return false
                }
                def unsupported = !(VersionMap.containsKey(params.api_version))
                if (unsupported) {
                    AA_TimerFilters.afterRequest(request, response, session)
                    logDetail(request, params.toString(), actionName, controllerName, 'api.error.api-version.unsupported')
                    apiService.renderErrorFormat(response,
                            [
                                    status: HttpServletResponse.SC_BAD_REQUEST,
                                    code: 'api.error.api-version.unsupported',
                                    args: [params.api_version, request.forwardURI, "Current version: "+API_CURRENT_VERSION]
                            ]
                        )
                    return false;
                }
                request.api_version = VersionMap[params.api_version]
                request['ApiRequestFilters.request.parameters.project']=params.project?:request.project?:''
                return true
            }
            after = {
                logDetail(request, request['ApiRequestFilters.request.parameters.project']?:'', actionName, controllerName)
            }
        }

        /**
         * check incubator features via feature toggle
         */
        incubator(uri:'/api/**'){
            before={
                def path= request.forwardURI.split('/')
                def feature = path.length > 4 && path[3] == 'incubator' ? path[4] : null
                def featurePresent={
                    def splat = grailsApplication.config.feature?.incubator?.getAt('*') in ['true', true]
                    splat || (grailsApplication.config?.feature?.incubator?.getAt(it) in ['true', true])
                }
                if (feature && !(featurePresent(feature))) {
                    apiService.renderErrorFormat(response,
                            [
                                    status: HttpServletResponse.SC_NOT_FOUND,
                                    code: 'api.error.invalid.request',
                                    args: [request.forwardURI]
                            ]
                    )
                    return false;
                }
            }
        }
    }

    private com.codahale.metrics.Timer.Context timer() {
        metricRegistry.timer(MetricRegistry.name('rundeck.api.requests', 'requestTimer')).time()
    }
}
