package rundeck.controllers

import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.plugins.ServiceProviderLoader
import com.dtolabs.rundeck.plugins.notification.NotificationPlugin
import com.dtolabs.rundeck.core.plugins.DescribedPlugin
import grails.testing.web.controllers.ControllerUnitTest
import rundeck.services.FrameworkService
import rundeck.services.PluginApiServiceSpec
import rundeck.services.PluginService
import spock.lang.Specification

class PluginControllerSpec extends Specification implements ControllerUnitTest<PluginController> {

    String fakePluginId = "fake".encodeAsSHA256().substring(0,12)

    def setup() {
    }

    def cleanup() {
    }

    void "plugin detail"() {
        given:
        controller.pluginService = Mock(PluginService)
        def fwksvc = Mock(FrameworkService)
        def fwk = Mock(Framework)
        fwksvc.getRundeckFramework() >> fwk
        fwk.getPluginManager() >> Mock(ServiceProviderLoader)
        controller.frameworkService = fwksvc
        controller.uiPluginService = Mock(UiPluginService)
        controller.pluginApiService = Mock(PluginApiService)

        when:
        def fakePluginDesc = new PluginApiServiceSpec.FakePluginDescription()
        params.name = "fake"
        params.service = "Notification"
        1 * controller.pluginService.getPluginDescriptor("fake", 'Notification') >> new DescribedPlugin(null,fakePluginDesc,"fake")
        1 * controller.frameworkService.rundeckFramework.pluginManager.getPluginMetadata(_,_) >> new PluginApiServiceSpec.FakePluginMetadata()
        1 * controller.uiPluginService.getPluginMessage('Notification','fake','plugin.title','Fake Plugin',_)>>'plugin.title'
        1 * controller.uiPluginService.getPluginMessage('Notification','fake','plugin.description','This is the best fake plugin',_)>>'plugin.description'
        1* controller.pluginApiService.pluginPropertiesAsMap('Notification','fake',_)>>[
                [name:'prop1',apiData:true],
                [name:'password',apiData:true]
        ]
        controller.pluginDetail()
        def rj = response.json
        def rp1 = rj.props.find { it.name == "prop1" }
        def rp2 = rj.props.find { it.name == "password" }

        then:
        rj.id == fakePluginId
        rj.name == "fake"
        rj.title == 'plugin.title'
        rj.desc == 'plugin.description'
        rp1.name == "prop1"
        rp1.apiData
        rp2.name == "password"
        rp2.apiData
    }
}
