package rundeck.controllers

import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.plugins.PluginMetadata
import com.dtolabs.rundeck.core.plugins.ServiceProviderLoader
import com.dtolabs.rundeck.core.plugins.configuration.Description
import com.dtolabs.rundeck.core.plugins.configuration.Property
import com.dtolabs.rundeck.plugins.notification.NotificationPlugin
import com.dtolabs.rundeck.plugins.util.PropertyBuilder
import com.dtolabs.rundeck.server.plugins.DescribedPlugin
import grails.testing.web.controllers.ControllerUnitTest
import rundeck.services.FrameworkService
import rundeck.services.PluginApiService
import rundeck.services.PluginApiServiceSpec
import rundeck.services.PluginService
import spock.lang.Specification

import java.text.SimpleDateFormat

class PluginControllerSpec extends Specification implements ControllerUnitTest<PluginController> {

    String fakePluginId = "Fake Plugin".encodeAsSHA256().substring(0,12)

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

        when:
        def fakePluginDesc = new PluginApiServiceSpec.FakePluginDescription()
        params.name = "fake"
        params.service = "Notification"
        1 * controller.pluginService.getPluginDescriptor("fake", NotificationPlugin.class) >> new DescribedPlugin(null,fakePluginDesc,"fake")
        1 * controller.frameworkService.rundeckFramework.pluginManager.getPluginMetadata(_,_) >> new PluginApiServiceSpec.FakePluginMetadata()
        controller.pluginDetail()
        def rj = response.json
        def rp1 = rj.props.find { it.name == "prop1" }
        def rp2 = rj.props.find { it.name == "password" }

        then:
        rj.artifactId == fakePluginId
        rj.name == "fake"
        rj.title == "Fake Plugin"
        rj.desc == "This is the best fake plugin"
        rp1.name == "prop1"
        rp1.title == "Property 1"
        rp1.desc == "A fake property for the fake plugin"
        rp1.defaultValue == "alpha"
        rp1.required == true
        rp1.allowed == ["alpha","beta","gamma"]
        rp2.name == "password"
        rp2.title == "Password"
        rp2.desc == "The password to the fake plugin"
        rp2.defaultValue == null
        rp2.required == false
        rp2.allowed == null
    }

}
