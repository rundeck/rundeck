package rundeck.services

import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import com.dtolabs.rundeck.core.plugins.ConfiguredPlugin
import com.dtolabs.rundeck.core.plugins.PluggableProviderService
import com.dtolabs.rundeck.core.plugins.configuration.PropertyResolver
import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope
import com.dtolabs.rundeck.plugins.webhook.WebhookData
import com.dtolabs.rundeck.plugins.webhook.WebhookEventPlugin
import com.dtolabs.rundeck.server.plugins.RundeckPluginRegistry
import com.dtolabs.rundeck.server.plugins.webhook.LogWebhookEventPlugin
import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import org.rundeck.app.authorization.RundeckAuthorizedServicesProvider
import org.rundeck.app.spi.Services
import rundeck.AuthToken
import rundeck.User
import rundeck.Webhook
import spock.lang.Specification

class WebhookServiceSpec extends Specification implements ServiceUnitTest<WebhookService>, DataTest {

    void setupSpec() {
        mockDomain User
        mockDomain AuthToken
        mockDomain Webhook
    }
    def setup() {
        User webhookUser = new User(login: "webhookUser")
        webhookUser.save()
    }

    def cleanup() {
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
        def mockLogWebhookPlugin = Mock(LogWebhookEventPlugin)

        when:
        def mockPropertyResolver = Mock(PropertyResolver)
        def webhookProviderService = Mock(PluggableProviderService)
        service.rundeckAuthorizedServicesProvider = Mock(RundeckAuthorizedServicesProvider) {
            getServicesWith(_) >> { Mock(Services)}
        }
        service.frameworkService = Mock(FrameworkService) {
            getFrameworkPropertyResolver(_,_) >> { mockPropertyResolver }
        }
        service.rundeckPluginRegistry = Mock(RundeckPluginRegistry) {
            createPluggableService(WebhookEventPlugin) >> {
                webhookProviderService
            }
        }

        service.pluginService = Mock(PluginService) {
            configurePlugin("log-webhook-event", _, _,PropertyScope.Instance) >> { new ConfiguredPlugin<WebhookEventPlugin>(mockLogWebhookPlugin,[:] ) }
        }
        service.processWebhook("log-webhook-event","{}",data,mockUserAuth)

        then:
        1 * mockLogWebhookPlugin.onEvent(_,{it.data.text=="my event data"})
    }

    def "save new webhook"() {
        given:
        User webhookUser = User.findByLogin("webhookUser")
        def mockUserAuth = Mock(UserAndRolesAuthContext) {
            getUsername() >> { "webhookUser" }
            getRoles() >> { ["webhook","test"] }
        }
        AuthToken authToken = new AuthToken(token: "TEST123",user: webhookUser,authRoles: "webhook,test",type:AuthToken.TOKEN_TYPE_WEBHOOK)
        service.apiService = Mock(ApiService) {
            generateAuthToken("webhookUser",_,_,null,true) >> { authToken }
        }

        when:
        def result = service.saveHook(mockUserAuth,[name:"test",user:"webhookUser",eventPlugin:"log-webhook-event","config":["cfg1":"val1"]])
        Webhook created = Webhook.findByName("test")

        then:
        result == [msg:"saved"]
        created.name == "test"
        created.eventPlugin == "log-webhook-event"
        created.authToken.user.login == "webhookUser"
        created.authToken.authRoles == "webhook,test"
        created.pluginConfigurationJson == '{"cfg1":"val1"}'

    }
}
