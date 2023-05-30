package rundeck.interceptors

import groovy.transform.CompileStatic
import org.apache.http.HttpStatus
import org.springframework.beans.factory.annotation.Autowired
import rundeck.services.ConfigurationService

import javax.servlet.http.HttpServletRequest

/**
 * Intercept all responses with 5xx status and removes stacktrace data
 */
@CompileStatic
class InternalErrorsInterceptor {
    private final ConfigurationService configurationService
    int order = HIGHEST_PRECEDENCE + 600

    @Autowired
    InternalErrorsInterceptor(ConfigurationService configurationService) {
        this.configurationService = configurationService
        matchAll().excludes {
            this.configurationService.getBoolean('feature', 'debug', 'showTracesOnResponse', true) || response == null
        }
    }

    boolean after() {
        if(response.getStatus() < HttpStatus.SC_INTERNAL_SERVER_ERROR)
            return true

        HttpServletRequest requestForRendering = request
        Enumeration<String> reqAttributes = requestForRendering.getAttributeNames()

        reqAttributes.each { String attributeName ->
            def attribute = requestForRendering.getAttribute(attributeName)
            if(attribute instanceof Throwable) {
                Throwable serverException = (attribute as Throwable)
                if (serverException) {
                    requestForRendering.removeAttribute(attributeName)
                    cleanStackTraces(serverException)
                    requestForRendering.setAttribute(attributeName, serverException)
                }
            }
        }

        render(view: '/error.gsp')
        return false
    }

    static void cleanStackTraces(Throwable exceptionToClean) {
        Throwable cause = exceptionToClean
        while (cause?.stackTrace != null && cause.stackTrace.size() > 0 ){
            cause.setStackTrace(new StackTraceElement[]{})
            cause = cause.getCause()
        }
    }
}