package rundeck.interceptors

import grails.testing.web.interceptor.InterceptorUnitTest
import rundeck.services.ConfigurationService
import spock.lang.Specification

class HealthInterceptorSpec extends Specification implements InterceptorUnitTest<HealthInterceptor> {

    def setup() {
    }

    def cleanup() {

    }

    void "health endpoint toggle"() {
        setup:
        interceptor.configurationService = Mock(ConfigurationService) {
            getBoolean("feature.healthEndpoint.enabled", true) >> toggle
        }
        when:
        boolean result = interceptor.before()

        then:
        result == expected

        where:
        toggle  | expected
        true    | true
        false   | false
    }
}
