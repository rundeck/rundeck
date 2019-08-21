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

import com.dtolabs.rundeck.core.authentication.tokens.AuthTokenManager
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import com.dtolabs.rundeck.core.plugins.ConfiguredPlugin
import com.dtolabs.rundeck.core.plugins.PluggableProviderService
import com.dtolabs.rundeck.core.plugins.PluginRegistry
import com.dtolabs.rundeck.core.plugins.configuration.PropertyResolver
import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope
import com.dtolabs.rundeck.core.webhook.WebhookEventException
import com.dtolabs.rundeck.plugins.webhook.WebhookData
import com.dtolabs.rundeck.plugins.webhook.WebhookEventContext
import com.dtolabs.rundeck.plugins.webhook.WebhookEventPlugin
import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import org.rundeck.app.spi.AuthorizedServicesProvider
import org.rundeck.app.spi.Services
import spock.lang.Specification


class WebhookServiceSpec extends Specification implements ServiceUnitTest<WebhookService>, DataTest {
    void setupSpec() {
        mockDomain Webhook
    }

    def "process webhook"() {
        given:
        def mockUserAuth = Mock(UserAndRolesAuthContext) {
            getUsername() >> { "webhookUser" }
            getRoles() >> { ["webhook","test"] }
        }
        WebhookData data = new WebhookData()
        data.webhook = "testhook"
        data.sender = "asender"
        data.contentType = "text/plain"
        data.data = new ByteArrayInputStream("my event data".bytes)

        when:
        def mockPropertyResolver = Mock(PropertyResolver)
        def webhookProviderService = Mock(PluggableProviderService)
        service.rundeckAuthorizedServicesProvider = Mock(AuthorizedServicesProvider) {
            getServicesWith(_) >> { Mock(Services)}
        }
        service.frameworkService = Mock(MockFrameworkService) {
            getFrameworkPropertyResolver(_,_) >> { mockPropertyResolver }
        }
        service.rundeckPluginRegistry = Mock(PluginRegistry) {
            createPluggableService(WebhookEventPlugin) >> {
                webhookProviderService
            }
        }
        TestWebhookEventPlugin testPlugin = new TestWebhookEventPlugin()

        service.pluginService = Mock(MockPluginService) {
            configurePlugin("log-webhook-event", _, _,
                            PropertyScope.Instance) >> { new ConfiguredPlugin<WebhookEventPlugin>(testPlugin, [:] ) }
        }
        service.processWebhook("log-webhook-event","{}",data,mockUserAuth)

        then:
        testPlugin.captured.data.text=="my event data"
    }

    class TestWebhookEventPlugin implements WebhookEventPlugin {
        WebhookData captured

        @Override
        void onEvent(final WebhookEventContext context, final WebhookData data) throws WebhookEventException {
            captured = data
        }
    }

    def "save new webhook"() {
        given:
        def mockUserAuth = Mock(UserAndRolesAuthContext) {
            getUsername() >> { "webhookUser" }
            getRoles() >> { ["webhook","test"] }
        }
        service.apiService = Mock(MockApiService)
        service.rundeckAuthTokenManagerService = Mock(AuthTokenManager) {
            parseAuthRoles(_) >> { ["webhook","test"] }
        }
        service.userService = Mock(MockUserService) {
            validateUserExists(_) >> { true }
        }

        when:
        def result = service.saveHook(mockUserAuth,[name:"test",project:"Test",user:"webhookUser",roles:"webhook,test",eventPlugin:"log-webhook-event","config":["cfg1":"val1"]])
        Webhook created = Webhook.findByName("test")

        then:
        result == [msg:"saved"]
        created.name == "test"
        created.project == "Test"
        created.eventPlugin == "log-webhook-event"
        created.pluginConfigurationJson == '{"cfg1":"val1"}'
        1 * service.apiService.generateUserToken(_,_,_,_,_,_) >> { [token:"12345"] }

    }

    def "ReplaceSecureOpts"() {
        given:
        def mockUserAuth = Mock(UserAndRolesAuthContext) {
            getUsername() >> { "webhookUser" }
            getRoles() >> { ["webhook","test"] }
        }
        when:
        def config = [:]
        config.prop1 = "my value"
        config.prop2 = '${KS:keys/proj1/sval}'
        config.prop3 = [
                prop1: 'foo',
                prop2: '${KS:keys/proj1/sval}'
        ]
        config.prop4 = [
                'foo',
                '${KS:keys/proj1/sval}'
        ]

        config.prop5 = '${KS:keys/proj1/sval} ${KS:keys/proj1/sval2}'

        def mockStorageTree = Mock(MockStorageTree) {
            hasPassword(_) >> { true }
            readPassword("keys/proj1/sval") >> { "password".bytes }
            readPassword("keys/proj1/sval2") >> { "other".bytes }
        }
        service.storageService = Mock(MockStorageService) {
            storageTreeWithContext(_) >> { mockStorageTree }
        }
        service.replaceSecureOpts(mockUserAuth,config)

        then:
        config.prop1 == "my value"
        config.prop2 == "password"
        config.prop3['prop2'] == "password"
        config.prop4[1] == "password"
        config.prop5 == "password other"

    }

    interface MockUserService {
        boolean validateUserExists(String user)
    }

    interface MockFrameworkService {
        PropertyResolver getFrameworkPropertyResolver(String project, Map config)
    }

    interface MockPluginService {
        ConfiguredPlugin configurePlugin(String pluginName, PluggableProviderService svc, PropertyResolver resolver, PropertyScope scope)
    }

    interface MockApiService {
        Map generateUserToken(UserAndRolesAuthContext ctx, Integer expiration, String user, Set<String> roles, boolean forceExpiration, boolean webhookToken)
    }

    interface MockStorageService {
        Object storageTreeWithContext(Object obj)
    }
    interface MockStorageTree {
        boolean hasPassword(String path)
        byte[] readPassword(String path)
    }
}
