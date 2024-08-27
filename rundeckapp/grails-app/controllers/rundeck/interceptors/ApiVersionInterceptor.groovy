package rundeck.interceptors

import com.codahale.metrics.MetricRegistry
import grails.converters.JSON
import grails.converters.XML
import org.grails.web.converters.exceptions.ConverterException
import org.grails.web.servlet.mvc.SynchronizerTokensHolder
import org.grails.web.util.WebUtils
import org.rundeck.app.web.RequestIdProvider
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import rundeck.controllers.TokenVerifierController

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

import static com.dtolabs.rundeck.app.api.ApiVersions.*


class ApiVersionInterceptor {
    int order = HIGHEST_PRECEDENCE + 25

    static final Logger logger = LoggerFactory.getLogger('org.rundeck.api.requests')
    private static final String METRIC_TIMER = 'ApiRequestFilters._METRIC_TIMER'
    private static final String REQUEST_TIME = 'ApiRequestFilters._TIMER'

    @Autowired
    MetricRegistry metricRegistry
    def messageSource
    def apiService
    @Autowired
    TokenVerifierController tokenVerifierController

    ApiVersionInterceptor() {
        match(uri: '/api/**')
    }

    def allowed_actions = ["renderError", "error"]

    static def logDetail(HttpServletRequest request, project, String action, String controller, String message = null) {
        request[METRIC_TIMER].stop()

        Map context = [
                remoteHost: request.remoteHost,
                requestId: request.getAttribute(RequestIdProvider.HTTP_ATTRIBUTE_NAME),
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
        context.each { MDC.put(it.key, it.value ? it.value.toString() : '') }
        try {
            logger.info(message ? message + context : context.toString())
        } finally {
            MDC.clear()
        }
    }

    boolean before() {
        request[REQUEST_TIME] = System.currentTimeMillis()
        request[METRIC_TIMER] = timer()
        
        def apiGuiRequest = request.remoteUser && null != session.api_access_allowed && !session.api_access_allowed
        if (request.method != 'GET') {
            boolean validToken = false
            if (params[SynchronizerTokensHolder.TOKEN_KEY]) {
                tokenVerifierController.withForm {
                    tokenVerifierController.refreshTokens()
                    validToken = true
                }
                if (!validToken) {
                    apiService.renderErrorFormat(response,
                        [
                                status: HttpServletResponse.SC_UNAUTHORIZED,
                                code: 'api.error.item.unauthorized',
                                args: [request.method, request.forwardURI]
                        ]
                    )
                    AA_TimerInterceptor.afterRequest(request, response, session)
                    logDetail(request, params.toString(), actionName, controllerName, 'api.error.item.unauthorized')
                    return false
                }
            }

            if (!validToken) {
                if (apiGuiRequest) {
                    log.debug("Api access request disallowed for ${request.forwardURI}")
                    response.sendError(HttpServletResponse.SC_NOT_FOUND)
                    return false
                } else if (null == session.api_access_allowed) {
                    session.api_access_allowed = true
                }
            }
        } else if (apiGuiRequest && request.getHeader('x-rundeck-ajax') != 'true') {
            response.sendError(HttpServletResponse.SC_NOT_FOUND)
            return false
        }
        
        if (controllerName == 'api' && allowed_actions.contains(actionName) || request.api_version) {
            request.is_allowed_api_request = true
            return true
        }

        if (!params.api_version) {
            flash.errorCode = 'api.error.api-version.required'
            AA_TimerInterceptor.afterRequest(request, response, session)
            logDetail(request, params.toString(), actionName, controllerName, 'api.error.api-version.required')
            apiService.renderErrorFormat(response,[code: 'api.error.api-version.required'])
            return false
        }
        def unsupported = !(VersionMap.containsKey(params.api_version))
        if (unsupported) {
            AA_TimerInterceptor.afterRequest(request, response, session)
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
        try {
            XML.use('v' + request.api_version)
            JSON.use('v' + request.api_version)
        } catch (ConverterException e) {
            //API request may have happened before named converter configs have been registered at startup
            //the response will be handled in AuthorizationInterceptor after the SetUserInterceptor completes
            request.apiVersionStatusNotReady=true
        }
        return true
    }

    boolean after() {
        logDetail(request, request['ApiRequestFilters.request.parameters.project']?:'', actionName, controllerName)
        return true
    }

    void afterView() {
        // no-op
    }

    private com.codahale.metrics.Timer.Context timer() {
        metricRegistry.timer(MetricRegistry.name('rundeck.api.requests', 'requestTimer')).time()
    }

}
