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
        '*'        | 'menu'        | 'apiExecutionsRunningv14' | false
        '*Project' | 'anyMenu'     | 'anyAction'               | true
    }

    void "excluded project"() {
        given:
        params.project = 'project1'
        request.requestURI >> '/api/15/executions/running'
        request.getMethod() >> 'GET'

        when:
        withRequest(controller: 'menu', action: 'apiExecutionsRunningv14')

        then:
        !interceptor.doesMatch()
    }
}

