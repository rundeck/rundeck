package rundeck.interceptors


class HealthInterceptor {

    def configurationService

    boolean before() {
        if(!configurationService.getBoolean("feature.healthEndpoint",true)) {
            response.status = 403
            return false
        }
        true
    }

    boolean after() { true }

    void afterView() {
        // no-op
    }
}
