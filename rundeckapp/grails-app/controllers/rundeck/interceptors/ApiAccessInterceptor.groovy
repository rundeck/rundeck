package rundeck.interceptors

/* Copied from:
 * ApiRequestFilters.java
 *
 * User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * Created: Feb 1, 2011 12:14:56 PM
 *
 * Copy made by Stephen Joyner Feb 2, 2018
 */
class ApiAccessInterceptor {

    int order = HIGHEST_PRECEDENCE + 24

    def allowed_pre_api_reqs=[
            'user':['login','loggedout'],
            'menu':['index','home'],
    ]

    ApiAccessInterceptor() {
        matchAll().excludes(uri: '/api/**')
    }

    /**
     * Disallow api access if a request comes for non-api url after login
     */
    boolean before() {
        if(InterceptorHelper.matchesStaticAssets(controllerName)) return true

        if(allowed_pre_api_reqs[controllerName] && (actionName in allowed_pre_api_reqs[controllerName])){
            return true
        }
        if (null == session.api_access_allowed) {
            log.debug("Disallowing API access, blocked due to request for ${controllerName}/${actionName}")
            session.api_access_allowed = false
        }
        true
    }

    boolean after() { true }

    void afterView() {
        // no-op
    }
}
