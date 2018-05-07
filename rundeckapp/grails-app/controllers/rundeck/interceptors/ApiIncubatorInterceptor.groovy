package rundeck.interceptors

import javax.servlet.http.HttpServletResponse


class ApiIncubatorInterceptor {

    int order = HIGHEST_PRECEDENCE + 26
    def apiService

    ApiIncubatorInterceptor() {
        match(uri: '/api/**')
    }

    /**
     * check incubator features via feature toggle
     */
    boolean before() {
        def path= request.forwardURI.split('/')
        def feature = path.length > 4 && path[3] == 'incubator' ? path[4] : null
        def featurePresent={
            def splat = grailsApplication.config.feature?.incubator?.getAt('*') in ['true', true]
            splat || (grailsApplication.config?.feature?.incubator?.getAt(it) in ['true', true])
        }
        if (feature && !(featurePresent(feature))) {
            apiService.renderErrorFormat(response,
                    [
                            status: HttpServletResponse.SC_NOT_FOUND,
                            code: 'api.error.invalid.request',
                            args: [request.forwardURI]
                    ]
            )
            return false;
        }
        return true
    }

    boolean after() { true }

    void afterView() {
        // no-op
    }
}
