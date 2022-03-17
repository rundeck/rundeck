package rundeck.interceptors


class HealthInterceptor {

    def configurationService

    boolean before() {
        if(!configurationService.getBoolean("feature.healthEndpoint.enabled",true)) {
            response.status = 404
            return false
        }
        true
    }

    boolean after() { true }

    void afterView() {
        // no-op
    }
}
