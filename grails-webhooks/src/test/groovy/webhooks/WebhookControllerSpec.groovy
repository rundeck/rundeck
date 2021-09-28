/*
 * Copyright 2019 Rundeck, Inc. (http://rundeck.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package webhooks

import com.dtolabs.rundeck.core.authorization.AuthContextEvaluator
import com.dtolabs.rundeck.core.authorization.AuthContextProvider
import com.dtolabs.rundeck.core.authorization.SubjectAuthContext
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import com.dtolabs.rundeck.plugins.webhook.DefaultJsonWebhookResponder
import com.dtolabs.rundeck.plugins.webhook.DefaultWebhookResponder
import com.dtolabs.rundeck.plugins.webhook.WebhookDataImpl
import com.dtolabs.rundeck.plugins.webhook.WebhookResponder
import grails.testing.web.controllers.ControllerUnitTest
import spock.lang.Specification

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


class WebhookControllerSpec extends Specification implements ControllerUnitTest<WebhookController> {

    def "post"() {
        given:
        controller.rundeckAuthContextProvider = Mock(AuthContextProvider)
        controller.rundeckAuthContextEvaluator = Mock(AuthContextEvaluator)
        controller.webhookService = Mock(MockWebhookService)

        when:
        params.authtoken = "1234"
        request.method = 'POST'
        controller.post()

        then:
        1 * controller.webhookService.getWebhookByToken(_) >> { new Webhook(name:"test",authToken: "1234")}
        1 * controller.rundeckAuthContextProvider.getAuthContextForSubjectAndProject(_, _) >> { new SubjectAuthContext(null, null) }
        1 * controller.rundeckAuthContextEvaluator.authorizeProjectResourceAny(_,_,_,_) >> { return true }
        1 * controller.webhookService.processWebhook(_,_,_,_,_) >> { webhookResponder }
        response.text == expectedMsg

        where:
        authtoken   | expectedMsg | webhookResponder
        "1234"      | 'ok' | new DefaultWebhookResponder()
        "1234#test" | '{"msg":"ok"}' | new DefaultJsonWebhookResponder([msg:"ok"])
    }

    def "post fail when not authorized"() {
        given:

        controller.rundeckAuthContextProvider = Mock(AuthContextProvider){
            getAuthContextForSubject(_) >> { new SubjectAuthContext(null,null) }
        }
        controller.rundeckAuthContextEvaluator = Mock(AuthContextEvaluator){
            authorizeApplicationResourceAny(_,_,_) >> { return false }
        }
        controller.webhookService = Mock(MockWebhookService)

        when:
        params.authtoken = "1234"
        request.method = 'POST'
        controller.post()

        then:
        1 * controller.webhookService.getWebhookByToken(_) >> { new Webhook(name:"test",authToken: "1234")}
        0 * controller.webhookService.processWebhook(_,_,_,_)
        response.text == '{"err":"You are not authorized to perform this action"}'
    }

    def "remove webhook should fail when project params is not present"() {
        given:
        controller.webhookService = Mock(MockWebhookService) {
            getWebhook(_) >> new Webhook()
        }

        controller.apiService = Mock(MockApiService)

        when:
        params.id = "1234"
        controller.remove()

        then:
        1 * controller.apiService.renderErrorFormat(_,[status: HttpServletResponse.SC_BAD_REQUEST,
                                                       code: 'api.error.parameter.required', args: ['project']])
        0 * controller.webhookService.delete(_)
    }

    def "503 if webhook is not enabled"() {
        given:
        controller.webhookService = Mock(MockWebhookService)

        when:
        params.authtoken = "1234"
        request.method = 'POST'
        controller.post()

        then:
        1 * controller.webhookService.getWebhookByToken(_) >> { new Webhook(name:"test",authToken: "1234",enabled:false)}
        0 * controller.webhookService.processWebhook(_,_,_,_)
        response.text == '{"err":"Webhook not enabled"}'
        response.status == 503
    }

    def "POST method is the only valid method"() {
        given:
        controller.rundeckAuthContextProvider = Mock(AuthContextProvider)
        controller.rundeckAuthContextEvaluator = Mock(AuthContextEvaluator)
        controller.webhookService = Mock(MockWebhookService)

        when:
        params.authtoken = "1234"
        request.method = method
        controller.post()

        then:
        invocations * controller.webhookService.getWebhookByToken(_) >> { new Webhook(name:"test",authToken: "1234",enabled:true)}
        invocations * controller.rundeckAuthContextProvider.getAuthContextForSubjectAndProject(_,_) >> { new SubjectAuthContext(null, null) }
        invocations * controller.rundeckAuthContextEvaluator.authorizeProjectResourceAny(_,_,_,_) >> { return true }
        invocations * controller.webhookService.processWebhook(_,_,_,_,_) >> { new DefaultWebhookResponder() }
        response.status == statusCode

        where:
        method      | statusCode | invocations
        'POST'      | 200        | 1
        'GET'       | 405        | 0
        'PUT'       | 405        | 0
        'DELETE'    | 405        | 0
    }

    interface MockWebhookService {
        Webhook getWebhookByToken(String token)
        Webhook getWebhook(Long id)
        WebhookResponder processWebhook(String pluginName, String pluginConfigJson, WebhookDataImpl data, UserAndRolesAuthContext context, HttpServletRequest request)
    }

    interface MockApiService {
        def renderErrorFormat(HttpServletResponse response, Map data)
    }
}
