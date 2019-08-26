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
import com.dtolabs.rundeck.core.authentication.tokens.AuthTokenType
import com.dtolabs.rundeck.core.authentication.tokens.AuthenticationToken
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import com.dtolabs.rundeck.core.plugins.ConfiguredPlugin
import com.dtolabs.rundeck.core.plugins.PluggableProviderService
import com.dtolabs.rundeck.core.plugins.PluginRegistry
import com.dtolabs.rundeck.core.plugins.ValidatedPlugin
import com.dtolabs.rundeck.core.plugins.configuration.PluginCustomConfigValidator
import com.dtolabs.rundeck.core.plugins.configuration.PropertyResolver
import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope
import com.dtolabs.rundeck.core.plugins.configuration.Validator
import com.dtolabs.rundeck.core.webhook.WebhookEventException
import com.dtolabs.rundeck.plugins.descriptions.PluginCustomConfig
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty
import com.dtolabs.rundeck.plugins.webhook.WebhookData
import com.dtolabs.rundeck.plugins.webhook.WebhookEventContext
import com.dtolabs.rundeck.plugins.webhook.WebhookEventPlugin
import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import org.rundeck.app.spi.AuthorizedServicesProvider
import org.rundeck.app.spi.Services
import spock.lang.Specification
import spock.lang.Unroll


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
            configurePlugin("test-webhook-event", _, _,
                            PropertyScope.Instance) >> { new ConfiguredPlugin<WebhookEventPlugin>(testPlugin, [:] ) }
        }
        service.processWebhook("test-webhook-event","{}",data,mockUserAuth)

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
        service.pluginService = Mock(MockPluginService) {
            validatePluginConfig(_,_,_) >> { return new ValidatedPlugin(report: new Validator.Report(),valid:true) }
            getPlugin(_,_) >> { new TestWebhookEventPlugin() }
            listPlugins(WebhookEventPlugin) >> { ["log-webhook-event":new TestWebhookEventPlugin()] }
        }

        when:
        def result = service.saveHook(mockUserAuth,[name:"test",project:"Test",user:"webhookUser",roles:"webhook,test",eventPlugin:"log-webhook-event","config":["cfg1":"val1"]])
        Webhook created = Webhook.findByName("test")

        then:
        result == [msg:"Saved webhook"]
        created.name == "test"
        created.project == "Test"
        created.eventPlugin == "log-webhook-event"
        created.pluginConfigurationJson == '{"cfg1":"val1"}'
        1 * service.apiService.generateUserToken(_,_,_,_,_,_) >> { [token:"12345"] }

    }

    @Unroll
    def "validate plugin config - custom validator"() {
        given:
        CustomConfigWebhookEventPlugin customPlugin = new CustomConfigWebhookEventPlugin()

        service.pluginService = Mock(MockPluginService) {
            validatePluginConfig(_,_,_) >> { return new ValidatedPlugin(report: new Validator.Report(),valid: true) }
            getPlugin(_,_) >> { customPlugin }
        }

        when:
        ValidatedPlugin vPlugin = service.validatePluginConfig("custom-cfg-webhook-event",config)

        then:
        vPlugin.valid == valid
        vPlugin.report.errors == errors


        where:
        valid | config                  | errors
        false | [:]                     | ["requiredProp":"requiredProp is required"]
        true  | [requiredProp:"value"]  | [:]
    }

    static class CustomConfigWebhookEventPlugin implements WebhookEventPlugin {
        WebhookData captured

        @PluginCustomConfig(validator = TestCustomValidator)
        Map config

        @Override
        void onEvent(final WebhookEventContext context, final WebhookData data) throws WebhookEventException {
            captured = data
        }
    }

    static class TestCustomValidator implements PluginCustomConfigValidator {

        @Override
        Validator.Report validate(final Map config) {
            Validator.Report report = new Validator.Report()
            if(!config.requiredProp) report.errors["requiredProp"] = "requiredProp is required"
            return report
        }
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

    def "import webhook"() {
        given:
        service.rundeckAuthTokenManagerService = Mock(AuthTokenManager)

        when:
        def result = service.importWebhook([name:"test",
                                            project:"Test",
                                            apiToken: [
                                                    token:"abc123",
                                                    user:"webhookUser",
                                                    creator:"admin",
                                                    roles:"webhook,test",
                                            ],
                                            eventPlugin:"log-webhook-event",
                                            "pluginConfiguration":'{"cfg1":"val1"}'])
        Webhook created = Webhook.findByName("test")

        then:
        result == [msg:"Webhook test imported"]
        created.name == "test"
        created.project == "Test"
        created.eventPlugin == "log-webhook-event"
        created.pluginConfigurationJson == '{"cfg1":"val1"}'
        1 * service.rundeckAuthTokenManagerService.importWebhookToken("abc123","admin","webhookUser","webhook,test") >> { true }
    }

    def "getWebhookWithAuth"() {
        setup:
        Webhook hook = new Webhook()
        hook.name = "hit"
        hook.project = "One"
        hook.authToken = "abc123"
        hook.eventPlugin = "do-some-action"
        hook.pluginConfigurationJson = '{"prop1":"true"}'
        hook.save()

        service.rundeckAuthTokenManagerService = Mock(AuthTokenManager) {
            getToken("abc123") >> { new TestAuthenticationToken(token:"abc123",
                                                                creator:"admin",
                                                                authRoles:"webhook,role1",
                                                                ownerName:"webhook") }
        }

        when:
        def output = service.getWebhookWithAuth(hook.id.toString())

        then:
        output.id == hook.id
        output.name == "hit"
        output.project == "One"
        output.eventPlugin == "do-some-action"
        output.config == ["prop1":"true"]
        output.user == "webhook"
        output.creator == "admin"
        output.roles == "webhook,role1"
        output.authToken == "abc123"
    }

    interface MockUserService {
        boolean validateUserExists(String user)
    }

    interface MockFrameworkService {
        PropertyResolver getFrameworkPropertyResolver(String project, Map config)
    }

    interface MockPluginService {
        ConfiguredPlugin configurePlugin(String pluginName, PluggableProviderService svc, PropertyResolver resolver, PropertyScope scope)
        ValidatedPlugin validatePluginConfig(String service, String pluginName, Map config)
        WebhookEventPlugin getPlugin(String service, Class pluginClass)
        Map listPlugins(Class pluginClass)
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

    class TestAuthenticationToken implements AuthenticationToken {

        String token
        String authRoles
        String uuid
        String creator
        String ownerName


        @Override
        AuthTokenType getType() {
            return AuthTokenType.WEBHOOK
        }

        @Override
        String getPrintableToken() {
            return token
        }

        @Override
        Date getExpiration() {
            return null
        }
    }
}
