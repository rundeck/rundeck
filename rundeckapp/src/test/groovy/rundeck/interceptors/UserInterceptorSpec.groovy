package rundeck.interceptors

import grails.testing.web.interceptor.InterceptorUnitTest
import org.grails.web.util.GrailsApplicationAttributes
import rundeck.services.ConfigurationService
import spock.lang.Specification

class UserInterceptorSpec extends Specification implements InterceptorUnitTest<UserInterceptor> {

    void "redirect to login uri when set"() {
        setup:
        interceptor.configurationService = Mock(ConfigurationService) {
            getString(_) >> redirectUri
        }

        when:
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, "login")
        boolean result = interceptor.before()
        
        then:
        expected == result
        response.redirectedUrl == redirectUri

        where:
        expected    | redirectUri
        false       | "redirect/login/here"
        true        | null

    }
}
