package rundeck.interceptors

import grails.testing.web.interceptor.InterceptorUnitTest
import spock.lang.Specification

/**
 * ApiProjectSelectInterceptor test
 */
class ApiProjectSelectInterceptorSpec extends Specification implements InterceptorUnitTest<ApiProjectSelectInterceptor> {

    void "validate execution running endpoints"() {
        given:
        params.project = project
        params.action = action
        params.controller = controller

        when:
        withRequest(controller: controller, action: action)

        then:
        interceptor.doesMatch() == match

        where:
        project    | controller    | action                    | match
        'xProject' | 'menu'        | 'apiExecutionsRunningv14' | true
        '*'        | 'menu'        | 'apiExecutionsRunningv14' | false
    }
}

