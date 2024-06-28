package rundeck.interceptors

import org.slf4j.MDC

class RequestIdInterceptor {
    int order = HIGHEST_PRECEDENCE + 1

    RequestIdInterceptor() {
        matchAll()
    }

    boolean before() {
        def requestId = UUID.randomUUID()

        request.setAttribute("requestId", requestId)
        MDC.put("requestId", requestId.toString())

        true
    }
}
