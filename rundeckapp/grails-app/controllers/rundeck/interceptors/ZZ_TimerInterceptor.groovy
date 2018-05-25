package rundeck.interceptors

class ZZ_TimerInterceptor {

    int order = HIGHEST_PRECEDENCE + 500

    ZZ_TimerInterceptor() {
        matchAll()
    }

    boolean before() { true }

    boolean after() {
        AA_TimerInterceptor.afterRequest(request,response,session)
        true
    }

    void afterView() {
        // no-op
    }
}
