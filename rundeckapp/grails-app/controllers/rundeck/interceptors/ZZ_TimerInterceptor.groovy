package rundeck.interceptors

import rundeck.filters.AA_TimerFilters


class ZZ_TimerInterceptor {

    ZZ_TimerInterceptor() {
        matchAll()
    }

    boolean before() { true }

    boolean after() {
        AA_TimerFilters.afterRequest(request,response,session)
        true
    }

    void afterView() {
        // no-op
    }
}
