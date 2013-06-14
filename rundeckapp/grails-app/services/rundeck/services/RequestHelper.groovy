package rundeck.services

import grails.util.GrailsWebUtil
import org.codehaus.groovy.grails.web.context.ServletContextHolder
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.support.WebApplicationContextUtils

/**
 * $INTERFACE is ...
 * User: greg
 * Date: 6/11/13
 * Time: 3:41 PM
 */
class RequestHelper {
    static Object doWithMockRequest(Closure clos) {

        def requestAttributes = RequestContextHolder.getRequestAttributes()
        boolean unbindrequest = false
        // outside of an executing request, establish a mock version
        if (!requestAttributes) {
            def servletContext = ServletContextHolder.getServletContext()
            def applicationContext = WebApplicationContextUtils.getRequiredWebApplicationContext(servletContext)
            requestAttributes = GrailsWebUtil.bindMockWebRequest(applicationContext)
            unbindrequest = true
        }

        //prep execution data
        def result
        try {
            result = clos.call()
        } finally {
            if (unbindrequest) {
                RequestContextHolder.setRequestAttributes(null)
            }
        }
        result
    }
}
