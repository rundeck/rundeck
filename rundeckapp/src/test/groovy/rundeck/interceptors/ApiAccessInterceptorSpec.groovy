package rundeck.interceptors

import grails.testing.web.interceptor.InterceptorUnitTest
import org.grails.spring.beans.factory.InstanceFactoryBean
import org.grails.web.util.GrailsApplicationAttributes
import org.rundeck.app.access.InterceptorHelper
import spock.lang.Specification

class ApiAccessInterceptorSpec extends Specification implements InterceptorUnitTest<ApiAccessInterceptor> {

    def setup() {
        def helperMock = Mock(InterceptorHelper)
        defineBeans {
            interceptorHelper(InstanceFactoryBean, helperMock)
        }
    }

    def cleanup() {

    }

    void "Test apiAccess interceptor matching"() {
        when: "A request matches the interceptor"
            withRequest(controller: "something")

        then: "The interceptor does match"
            interceptor.doesMatch()
    }

    void "Test apiAccess interceptor does not match api "() {
        when: "A request matches the api"
            withRequest(uri: "/api/apiVersion")

        then: "The interceptor does not match"
            !interceptor.doesMatch()
    }

    void "Test apiAccess before disables api access "() {
        given:
            session.api_access_allowed = preset
        when: "A request matches the api"
            withRequest(controller: 'something', action: 'other')
            interceptor.before()

        then: "api_access_allowed set to false"
            session.api_access_allowed == expect
        where:
            preset | expect
            true   | true
            false  | false
            null   | false
    }

    void "Test apiAccess before allowed actions "() {
        given:
            session.api_access_allowed = null
            withRequest(controller: ctrl, action: action)
            request.setAttribute(GrailsApplicationAttributes.CONTROLLER_NAME_ATTRIBUTE, ctrl)
            request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, action)
        when: "A request matches the api"

            def result = interceptor.before()

        then: "api_access_allowed unset"
            session.api_access_allowed == expect
            result
        where:
            ctrl    | action        | expect
            'menu'  | 'index'       | null
            'menu'  | 'home'        | null
            'menu'  | 'other'       | false
            'user'  | 'login'       | null
            'user'  | 'loggedout'   | null
            'user'  | 'other'       | false
            'error' | 'fiveHundred' | null
            'error' | 'other'       | false

    }
}
