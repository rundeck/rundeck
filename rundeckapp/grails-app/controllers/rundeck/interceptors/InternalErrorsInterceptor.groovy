package rundeck.interceptors

import groovy.transform.CompileStatic
import org.apache.http.HttpStatus

import javax.servlet.http.HttpServletRequest

/**
 * Intercept all responses with 5xx status and removes stacktrace data
 */
@CompileStatic
class InternalErrorsInterceptor {
    private static final String SHOW_TRACES_DEBUG_PROP_NAME = 'rundeck.feature.debug.showTracesOnResponse'
    private static final String[] EXCEPT_ATTR_NAMES = ['javax.servlet.error.exception', 'exception']
    int order = HIGHEST_PRECEDENCE + 600

    InternalErrorsInterceptor() {
        matchAll().excludes {
            grailsApplication.config.getProperty(SHOW_TRACES_DEBUG_PROP_NAME, 'false') == 'true' ||
            response == null ||
            response.getStatus() < HttpStatus.SC_INTERNAL_SERVER_ERROR
        }
    }

    boolean after() {
        HttpServletRequest requestForRendering = request

        for(String exceptionAttrName: EXCEPT_ATTR_NAMES){
            Throwable serverException = (requestForRendering.getAttribute(exceptionAttrName) as Throwable)
            if(serverException) {
                requestForRendering.removeAttribute(exceptionAttrName)
                cleanStackTraces(serverException)
                requestForRendering.setAttribute(exceptionAttrName, serverException)
            }
        }

        return true
    }

    static void cleanStackTraces(Throwable exceptionToClean) {
        Throwable cause = exceptionToClean
        while (cause?.stackTrace != null && cause.stackTrace.size() > 0 ){
            cause.setStackTrace(new StackTraceElement[]{})
            cause = cause.getCause()
        }
    }
}