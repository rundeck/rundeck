package rundeck.interceptors

import grails.testing.web.interceptor.InterceptorUnitTest
import rundeck.controllers.MenuController
import rundeck.services.ApiService
import rundeck.services.FrameworkService
import spock.lang.Specification

/**
 * ApiProjectSelectInterceptor test
 */
class ApiProjectSelectInterceptorSpec extends Specification implements InterceptorUnitTest<ApiProjectSelectInterceptor> {

    void "excluded wildcard on projects"() {
        given:
        params.project = project
        params.action = action
        params.controller = controller
        request.requestURI = '/api/apiVersion'

        when:
        withRequest(controller: controller, action: action)

        then:
        interceptor.doesMatch() == match

        where:
        project    | controller    | action                    | match
        'xProject' | 'menu'        | 'apiExecutionsRunningv14' | true
        '*'        | 'menu'        | 'apiExecutionsRunningv14' | false
        '***'      | 'menu'        | 'apiExecutionsRunningv14' | true
        '*Project' | 'menu'        | 'apiExecutionsRunningv14' | true
        '*Project' | 'anyMenu'     | 'anyAction'               | true
    }
}

