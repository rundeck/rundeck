package rundeck.interceptors

class ZZ_TimerInterceptor {

    int order = HIGHEST_PRECEDENCE + 500
    def configurationService

    ZZ_TimerInterceptor() {
        matchAll()
    }

    boolean before() { true }

    boolean after() {
        def ignorePrefixes = configurationService.getValue("web.logging.ignorePrefixes", [])
        String testUri = request[AA_TimerInterceptor._REQ_URI]
        if(ignorePrefixes.any { testUri.startsWith(it) }) {
            return true
        }
        AA_TimerInterceptor.afterRequest(request,response,session)
        true
    }

    void afterView() {
        // no-op
    }
}
