package rundeck.controllers

import com.dtolabs.rundeck.core.common.Framework

import org.grails.plugins.testing.GrailsMockMultipartFile
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
import rundeck.services.FrameworkService
import spock.lang.Specification

class PluginControllerSpec extends Specification implements ControllerUnitTest<PluginController> {

    String fakePluginId = "fake".encodeAsSHA256().substring(0,12)
    static final String PLUGIN_FILE = "rundeck-ui-plugin-examples-1.0-plugin.zip"

    File uploadTestBaseDir = File.createTempDir()
    File uploadTestTargetDir = File.createTempDir()

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
        1 * controller.uiPluginService.getProfileFor('Notification','fake')>>[:]
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

    void "plugin detail no plugin or framework service"() {
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

            controller.pluginDetail()

        then:
            response.status == 404
            1 * controller.pluginService.getPluginDescriptor("fake", 'Notification') >> null
            1 * controller.frameworkService.rundeckFramework.getService('Notification') >> null

    }

    def "plugin service descriptions"() {
        given:
        controller.pluginService = Mock(PluginService)
        controller.uiPluginService = Mock(UiPluginService)
        def fakePluginDesc1 = new PluginApiServiceSpec.FakePluginDescription()
        fakePluginDesc1.name = 'XYZfake'
        def fakePluginDesc2 = new PluginApiServiceSpec.FakePluginDescription()
        fakePluginDesc2.name = 'ABCfake'
        request.addHeader('x-rundeck-ajax', 'true')
        params.service = svcName
        messageSource.addMessage(
                "framework.service.${svcName}.label",
                Locale.ENGLISH,
                "framework.service.${svcName}.label"
        )
        messageSource.addMessage(
                "framework.service.${svcName}.label.indexed",
                Locale.ENGLISH,
                "framework.service.${svcName}.label.indexed"
        )
        messageSource.addMessage(
                "framework.service.${svcName}.label.plural",
                Locale.ENGLISH,
                "framework.service.${svcName}.label.plural"
        )
        messageSource.addMessage(
                "framework.service.${svcName}.add.title",
                Locale.ENGLISH,
                "framework.service.${svcName}.add.title"
        )
        when:
        def result = controller.pluginServiceDescriptions(svcName)
        then:
        1 * controller.pluginService.getPluginTypeByService(svcName) >> NotificationPlugin
        1 * controller.pluginService.listPlugins(NotificationPlugin) >> [
                XYZfake: new DescribedPlugin<NotificationPlugin>(null, fakePluginDesc1, 'XYZfake'),
                ABCfake: new DescribedPlugin<NotificationPlugin>(null, fakePluginDesc2, 'ABCfake')
            ]
            1 * controller.uiPluginService.getPluginMessage(svcName, 'ABCfake', 'plugin.title', _, _) >>
            'ABC title'
            1 * controller.uiPluginService.getPluginMessage(svcName, 'ABCfake', 'plugin.description', _, _) >>
            'ABC desc'
            1 * controller.uiPluginService.getPluginMessage(svcName, 'XYZfake', 'plugin.title', _, _) >>
            'XYZ title'
            1 * controller.uiPluginService.getPluginMessage(svcName, 'XYZfake', 'plugin.description', _, _) >>
            'XYZ desc'
            1 * controller.uiPluginService.getProfileFor(svcName,'XYZfake')>>[:]
            1 * controller.uiPluginService.getProfileFor(svcName,'ABCfake')>>[:]
            def json = response.json
            json.service == svcName
            json.descriptions
            json.descriptions == [
                [
                        name       : 'ABCfake',
                        title      : 'ABC title',
                        description: 'ABC desc'
                ],
                [
                        name       : 'XYZfake',
                        title      : 'XYZ title',
                        description: 'XYZ desc'
                ],
        ]
        json.labels
        json.labels.singular == 'framework.service.Notification.label'
        json.labels.indexed == 'framework.service.Notification.label.indexed'
        json.labels.plural == 'framework.service.Notification.label.plural'
        json.labels.addButton == 'framework.service.Notification.add.title'

        where:
        svcName        | _
        'Notification' | _
    }

    void "upload plugin no file specified"() {
        setup:
        controller.frameworkService = Mock(FrameworkService)

        when:
        controller.uploadPlugin()

        then:
        1 * controller.frameworkService.getAuthContextForSubject(_)
        1 * controller.frameworkService.authorizeApplicationResourceType(_,_,_) >> true
        response.redirectUrl == "/menu/plugins"
        flash.errors == ["plugin.error.missing.upload.file"]
    }

    void "install plugin no plugin url specified"() {
        setup:
        controller.frameworkService = Mock(FrameworkService)

        when:
        controller.installPlugin()

        then:
        1 * controller.frameworkService.getAuthContextForSubject(_)
        1 * controller.frameworkService.authorizeApplicationResourceType(_,_,_) >> true
        response.redirectUrl == "/menu/plugins"
        flash.errors == ["plugin.error.missing.url"]
    }

    void "upload plugin"() {
        setup:
        File uploaded = new File(uploadTestTargetDir,PLUGIN_FILE)
        def fwksvc = Mock(FrameworkService)
        def fwk = Mock(Framework) {
            getBaseDir() >> uploadTestBaseDir
            getLibextDir() >> uploadTestTargetDir
        }
        fwksvc.getRundeckFramework() >> fwk
        controller.frameworkService = fwksvc


        when:
        !uploaded.exists()
        def pluginInputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(PLUGIN_FILE)
        request.addFile(new GrailsMockMultipartFile("pluginFile",PLUGIN_FILE,"application/octet-stream",pluginInputStream))
        controller.uploadPlugin()

        then:
        1 * controller.frameworkService.getAuthContextForSubject(_)
        1 * controller.frameworkService.authorizeApplicationResourceType(_,_,_) >> true
        response.redirectUrl == "/menu/plugins"
        flash.installSuccess
        uploaded.exists()

        cleanup:
        uploaded.delete()
    }

    void "install plugin"() {
        setup:
        File installed = new File(uploadTestTargetDir,PLUGIN_FILE)
        def fwksvc = Mock(FrameworkService)
        def fwk = Mock(Framework) {
            getBaseDir() >> uploadTestBaseDir
            getLibextDir() >> uploadTestTargetDir
        }
        fwksvc.getRundeckFramework() >> fwk
        controller.frameworkService = fwksvc

        when:
        !installed.exists()
        def pluginUrl = Thread.currentThread().getContextClassLoader().getResource(PLUGIN_FILE)
        params.pluginUrl = pluginUrl.toString()
        controller.installPlugin()

        then:
        1 * controller.frameworkService.getAuthContextForSubject(_)
        1 * controller.frameworkService.authorizeApplicationResourceType(_,_,_) >> true
        response.redirectUrl == "/menu/plugins"
        flash.installSuccess
        installed.exists()

        cleanup:
        installed.delete()
    }

    void "unauthorized install plugin fails"() {
        setup:
        controller.frameworkService = Mock(FrameworkService)
        
        when:
        def pluginUrl = Thread.currentThread().getContextClassLoader().getResource(PLUGIN_FILE)
        params.pluginUrl = pluginUrl.toString()
        controller.installPlugin()

        then:
        1 * controller.frameworkService.getAuthContextForSubject(_)
        1 * controller.frameworkService.authorizeApplicationResourceType(_,_,_) >> false
        response.redirectUrl == "/menu/plugins"
        flash.errors == ["request.error.unauthorized.title"]
    }
}
