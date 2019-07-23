package rundeck.controllers

import com.dtolabs.rundeck.core.authorization.SubjectAuthContext
import grails.testing.web.controllers.ControllerUnitTest
import rundeck.AuthToken
import rundeck.Webhook
import rundeck.services.FrameworkService
import rundeck.services.WebhookService
import spock.lang.Specification

class WebhookControllerSpec extends Specification implements ControllerUnitTest<WebhookController> {

    def setup() {
    }

    def cleanup() {
    }

    def "post"() {
        given:
        controller.frameworkService = Mock(FrameworkService) {
            getAuthContextForSubject(_) >> { new SubjectAuthContext(null,null) }
            authorizeApplicationResourceAny(_,_,_) >> { return true }
        }
        controller.webhookService = Mock(WebhookService) {
            getWebhook(_) >> { new Webhook(name:"test",authToken: new AuthToken(token:"1234"))}
            processWebhook(_,_,_,_) >> {}
        }

        when:
        params.name = "test"
        params.authtoken = "1234"
        controller.post()

        then:
        response.text == '{"msg":"ok"}'
    }

    def "post fail with incorrect auth token"() {
        given:
        controller.frameworkService = Mock(FrameworkService) {
            getAuthContextForSubject(_) >> { new SubjectAuthContext(null,null) }
            authorizeApplicationResourceAny(_,_,_) >> { return true }
        }
        controller.webhookService = Mock(WebhookService) {
            getWebhook(_) >> { new Webhook(name:"test",authToken: new AuthToken(token:"1234"))}
            processWebhook(_,_,_,_) >> {}
        }

        when:
        params.name = "test"
        params.authtoken = "XYZ"
        controller.post()

        then:
        response.text == '{"err":"Invalid webhook token"}'
    }

    def "post fail when not authorized"() {
        given:
        controller.frameworkService = Mock(FrameworkService) {
            getAuthContextForSubject(_) >> { new SubjectAuthContext(null,null) }
            authorizeApplicationResourceAny(_,_,_) >> { return false }
        }
        controller.webhookService = Mock(WebhookService) {
            getWebhook(_) >> { new Webhook(name:"test",authToken: new AuthToken(token:"1234"))}
            processWebhook(_,_,_,_) >> {}
        }

        when:
        params.name = "test"
        params.authtoken = "1234"
        controller.post()

        then:
        response.text == '{"err":"You are not authorized to perform this action"}'
    }
}
