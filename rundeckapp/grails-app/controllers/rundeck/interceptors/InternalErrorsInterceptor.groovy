package rundeck.interceptors

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.apache.http.HttpStatus
import org.springframework.beans.factory.annotation.Autowired
import rundeck.services.ConfigurationService

import javax.servlet.http.HttpServletRequest

/**
 * Intercept all responses with 5xx status and removes stacktrace data
 */
@Slf4j
@CompileStatic
class InternalErrorsInterceptor {
    private final ConfigurationService configurationService
    int order = HIGHEST_PRECEDENCE + 600

    @Autowired
    InternalErrorsInterceptor(ConfigurationService configurationService) {
        this.configurationService = configurationService
        matchAll().excludes {
            this.configurationService.getBoolean('feature', 'debug', 'showTracesOnResponse', false) || response == null
        }
    }

    boolean after() {
        if(response.getStatus() < HttpStatus.SC_INTERNAL_SERVER_ERROR)
            return true

        HttpServletRequest requestForRendering = request
        Enumeration<String> reqAttributes = requestForRendering.getAttributeNames()
        boolean throwableFound = false

        log.error(":::::::::::::::::::::::::::::ENTERING ERRORS INTERCEPTOR:::::::::::::")
        log.error("Request URI: " + request.getRequestURI())
        log.error("Status: " + response.getStatus())
        log.error("response: " + response.getOutputStream().toString())
        log.error("---------------------------------------------------------------------")

        reqAttributes.each { String attributeName ->
            def attribute = requestForRendering.getAttribute(attributeName)
            if(attribute instanceof Throwable && attribute != null) {
                throwableFound = true
                Throwable serverException = (attribute as Throwable)
                log.error("Has a throwable instance!")
                log.error("Attr Name: " + attributeName)
                log.error("SERVER ERROR:::::::::: ")
                attribute.printStackTrace()

                requestForRendering.removeAttribute(attributeName)
                cleanStackTraces(serverException)
                requestForRendering.setAttribute(attributeName, serverException)
            }
        }

        log.error(":::::::::::::::::::::::::::::EXITING ERRORS INTERCEPTOR:::::::::::::")
        if(throwableFound)
            render(view: '/error.gsp')
        return !throwableFound
    }

    static void cleanStackTraces(Throwable exceptionToClean) {
        Throwable cause = exceptionToClean
        while (cause?.stackTrace != null && cause.stackTrace.size() > 0 ){
            cause.setStackTrace(new StackTraceElement[]{})
            cause = cause.getCause()
        }
    }
}