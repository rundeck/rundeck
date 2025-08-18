package rundeck.interceptors

import grails.testing.web.interceptor.InterceptorUnitTest
import spock.lang.Specification

class PostApiTokenInterceptorSpec extends Specification implements InterceptorUnitTest<PostApiTokenInterceptor> {

    def setup() {
    }

    def cleanup() {

    }

    void "Test postApiToken interceptor matching"() {
        when:"A request matches the interceptor"
            withRequest(controller:"postApiToken")

        then:"The interceptor does match"
            interceptor.doesMatch()
    }

    void "after method does nothing when request has no authenticated token"() {
        given: "A request without an authenticated token and a session with a user"
        request.authenticatedToken = false
        session.user = "testUser"
        session.subject = "testSubject"

        when: "The after method is called"
        boolean result = interceptor.after()

        then: "The session and request attributes remain unchanged"
        result
        session.user == "testUser"
        session.subject == "testSubject"
        request.subject == null
    }


    void "after method does nothing when session user is null"() {
        given: "A request with an authenticated token and a session without a user"
        request.authenticatedToken = true
        session.user = null
        session.subject = "testSubject"

        when: "The after method is called"
        boolean result = interceptor.after()

        then: "The session and request attributes remain unchanged"
        result
        session.subject == "testSubject"
        request.subject == null
    }

    void "after method clears session and request attributes when conditions are met"() {
        given: "A request with an authenticated token and a session with a user"
        request.authenticatedToken = true
        session.user = "testUser"
        session.subject = "testSubject"


        when: "The after method is called"
        boolean result = interceptor.after()

        then: "The session and request attributes are cleared and the session is invalidated"
        result
        session.user == null
        session.subject == null
        request.subject == null
    }
}
