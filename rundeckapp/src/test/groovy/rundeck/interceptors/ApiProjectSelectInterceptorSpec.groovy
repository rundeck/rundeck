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
        'xProject' | 'menu'        | 'apiExecutionsRunningv14' | false
        '*'        | 'menu'        | 'apiExecutionsRunningv14' | false
        '***'      | 'menu'        | 'apiExecutionsRunningv14' | false
        '*Project' | 'menu'        | 'apiExecutionsRunningv14' | false
        '*Project' | 'anyMenu'     | 'anyAction'               | true
    }

    void "excluded project"() {
        given:
        params.project = 'project1'
        request.requestURI >> '/api/47/executions/running'
        request.getMethod() >> 'GET'

        when:
        withRequest(controller: 'menu', action: 'apiExecutionsRunningv14')

        then:
        !interceptor.doesMatch()
    }

    def "api project delete is allowed when the project is disabled"() {
        given:
        params.project = 'myproject'
        interceptor.frameworkService = Mock(FrameworkService) {
            existsFrameworkProject('myproject') >> true
            isFrameworkProjectDisabled('myproject') >> true
        }
        interceptor.apiService = Mock(ApiService)

        when:
        webRequest.controllerName = 'project'
        webRequest.actionName = 'apiProjectDelete'
        def allowed = interceptor.before()

        then:
        allowed
        0 * interceptor.apiService.renderErrorFormat(*_)
    }

    def "other api requests are rejected when the project is disabled"() {
        given:
        params.project = 'myproject'
        interceptor.frameworkService = Mock(FrameworkService) {
            existsFrameworkProject('myproject') >> true
            isFrameworkProjectDisabled('myproject') >> true
        }
        interceptor.apiService = Mock(ApiService)

        when:
        webRequest.controllerName = 'project'
        webRequest.actionName = 'apiProjectConfig'
        def allowed = interceptor.before()

        then:
        !allowed
        1 * interceptor.apiService.renderErrorFormat(_, { it.code == 'api.error.project.disabled' })
    }
}

