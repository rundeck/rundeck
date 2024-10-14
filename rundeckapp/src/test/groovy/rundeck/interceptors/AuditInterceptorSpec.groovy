package rundeck.interceptors

import grails.testing.web.interceptor.InterceptorUnitTest
import rundeck.services.ConfigurationService
import spock.lang.Specification

class AuditInterceptorSpec  extends Specification implements InterceptorUnitTest<AuditInterceptor> {
    def "Skip AuditInterceptor if requesting fiveHundred action on error page"() {
        given:
        withRequest(controller: 'error', action: 'fiveHundred')
        defineBeans {
            configurationService(ConfigurationService) {
                grailsApplication = grailsApplication
            }
        }
        when: "The interceptor check if request matches the interceptor"

        def result = interceptor.doesMatch()

        then: "Should not matches"
        !result

    }
}
