package rundeck.interceptors

import groovy.transform.CompileStatic
import org.rundeck.app.web.RequestIdProvider
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired

@CompileStatic
class RequestIdInterceptor {
    int order = HIGHEST_PRECEDENCE + 1

    @Autowired
    RequestIdProvider requestIdProvider

    RequestIdInterceptor() {
        matchAll()
    }

    boolean before() {
        def requestId = requestIdProvider.getRequestId(request)

        request.setAttribute(RequestIdProvider.HTTP_ATTRIBUTE_NAME, requestId)
        MDC.put("requestId", requestId)

        true
    }
}
