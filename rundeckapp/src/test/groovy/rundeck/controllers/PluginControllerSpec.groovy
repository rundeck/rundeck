package rundeck.controllers

import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.plugins.ServiceProviderLoader
import com.dtolabs.rundeck.core.plugins.ValidatedPlugin
import com.dtolabs.rundeck.core.plugins.configuration.Validator
import com.dtolabs.rundeck.plugins.notification.NotificationPlugin
import com.dtolabs.rundeck.core.plugins.DescribedPlugin
import grails.testing.web.controllers.ControllerUnitTest
import rundeck.services.FrameworkService
import rundeck.services.PluginApiService
import rundeck.services.PluginApiServiceSpec
import rundeck.services.PluginService
import rundeck.services.UiPluginService
import spock.lang.Specification

class PluginControllerSpec extends Specification implements ControllerUnitTest<PluginController> {

    String fakePluginId = "fake".encodeAsSHA256().substring(0,12)

    def setup() {
    }

    def cleanup() {
    }
    static final String TEST_JSON1 = '''{"config":{"actions._indexes":"dbd3da9c_1","actions._type":"list",
"actions.entry[dbd3da9c_1].type":"testaction1","actions.entry[dbd3da9c_1].config.actions._type":"embedded",
"actions.entry[dbd3da9c_1].config.actions.type":"","actions.entry[dbd3da9c_1].config.actions.config.stringvalue":"asdf",
"actions.entry[dbd3da9c_1].config.actions":"{stringvalue=asdf}"},"report":{}}'''

    void "validate"() {
        given:
            request.content = json.bytes
            request.contentType = 'application/json'
            request.method = 'POST'
            request.addHeader('x-rundeck-ajax', 'true')
            def project = 'Aproject'
            def service = 'AService'
            def name = 'someproperty'
            params.project = project
            params.service = service
            params.name = name
            controller.pluginService = Mock(PluginService)
        when:
            def result = controller.pluginPropertiesValidateAjax( service, name)
        then:
            1 * controller.pluginService.validatePluginConfig(service, name, expected/*, project*/) >>
            new ValidatedPlugin(valid: true, report: Validator.buildReport().build())
            0 * controller.pluginService._(*_)
            response.status == 200
            response.json != null
            response.json.valid == true
        where:
            json            | expected
            '{"config":{}}' | [:]
            TEST_JSON1      | [actions: [[type: 'testaction1', config: [actions: [stringvalue: 'asdf']]]]]
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
