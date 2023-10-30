package rundeck.interceptors

import grails.web.servlet.mvc.GrailsParameterMap
import groovy.transform.builder.Builder

import javax.servlet.http.HttpServletResponse


/**
 * Validates project exists and is enabled for all api calls.
 */
class ApiProjectSelectInterceptor {
    int order = HIGHEST_PRECEDENCE + 27

    def apiService
    def frameworkService

    Closure<Boolean> projectWithWildcard = {
        WildcardValidator validator = WildcardValidator.builder()
                .action('apiExecutionsRunningv14')
                .controller('menu')
                .project('*')
                .build()

        for (property in validator.keyProps()) {
            def res = validator.validate(property, validator[property] as String, params)
            if (!validator.validate(property, validator[property] as String, params)) {
                return false
            }
        }
        return true
    }

    ApiProjectSelectInterceptor() {
        match(uri: '/api/**')
                .excludes(controller: 'project', action: 'apiProjectCreate', method: 'POST')
        //match(controller: 'menu')
                .excludes(projectWithWildcard)
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
                        status: HttpServletResponse.SC_NOT_FOUND,
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

    @Builder
    class WildcardValidator {
        String controller
        String project
        String action

        boolean validate(String param, String value, GrailsParameterMap map) {
            return map[param] && map[param] == value
        }

        Set<String> keyProps() {
            return getProperties().keySet().findAll {prop -> prop != 'class'}
        }
    }
}
