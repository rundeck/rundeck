package rundeck.interceptors

import com.codahale.metrics.MetricRegistry
import grails.testing.web.interceptor.InterceptorUnitTest
import spock.lang.Specification

class ApiVersionInterceptorSpec extends Specification implements InterceptorUnitTest<ApiVersionInterceptor> {

    def setup() {
        defineBeans {
            metricRegistry(MetricRegistry)
        }
    }

    def cleanup() {

    }

    void "Test apiVersion interceptor matching"() {
        when:"A request matches the interceptor"
            withRequest(uri:"/api/apiVersion")

        then:"The interceptor does match"
            interceptor.doesMatch()
    }
}
