package webhooks

import grails.testing.web.interceptor.InterceptorUnitTest
import org.grails.web.util.GrailsApplicationAttributes
import spock.lang.Specification
import spock.lang.Unroll

class WebhookInterceptorSpec extends Specification implements InterceptorUnitTest<WebhookInterceptor> {

    def setup() {
    }

    def cleanup() {

    }

    @Unroll
    void "Webhook allowed methods"() {
        when:
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, action)
        withRequest(controller:"webhook")

        then:
        interceptor.doesMatch()
        expected == interceptor.before()
        response.status == code

        where:
        action | code | expected
        'post' | 200  | true
        null   | 405  | false
    }
}
