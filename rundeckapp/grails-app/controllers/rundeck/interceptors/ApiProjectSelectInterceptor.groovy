package rundeck.interceptors

import javax.servlet.http.HttpServletResponse


/**
 * Validates project exists and is enabled for all api calls.
 */
class ApiProjectSelectInterceptor {
    int order = HIGHEST_PRECEDENCE + 27

    def apiService
    def frameworkService

    ApiProjectSelectInterceptor() {
        match(uri: '/api/**')
                .excludes(controller: 'project', action: 'apiProjectCreate', method: 'POST')
    }

    /**
     * Check if project parameters exists and if so then validate.
     */
    boolean before() {
        if (params.project) {

            String project = params.project

            if (!frameworkService.existsFrameworkProject(project)) {
                apiService.renderErrorFormat(response, [
                        status: HttpServletResponse.SC_NOT_FOUND,
                        code  : 'api.error.project.missing',
                        args  : [params.project]
                ])
                return false
            }
            if (frameworkService.isFrameworkProjectDisabled(project)) {
                apiService.renderErrorFormat(response, [
                        status: HttpServletResponse.SC_CONFLICT,
                        code  : 'api.error.project.disabled',
                        args  : [params.project]
                ])
                return false
            }
        }
        return true
    }

    boolean after() { true }

    void afterView() {
        // no-op
    }
}
