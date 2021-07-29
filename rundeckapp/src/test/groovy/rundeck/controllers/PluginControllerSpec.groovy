package rundeck.controllers

import com.dtolabs.rundeck.core.authorization.AuthContextProvider
import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.common.IFramework
import com.dtolabs.rundeck.core.config.FeatureService
import com.dtolabs.rundeck.core.plugins.PluginMetadata
import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope
import com.dtolabs.rundeck.plugins.rundeck.UIPlugin
import com.dtolabs.rundeck.plugins.util.DescriptionBuilder
import com.dtolabs.rundeck.server.plugins.services.UIPluginProviderService
import grails.util.Described
import org.apache.logging.log4j.core.config.plugins.util.PluginManager
import org.grails.plugins.testing.GrailsMockMultipartFile
import com.dtolabs.rundeck.core.plugins.ServiceProviderLoader
import com.dtolabs.rundeck.core.plugins.ValidatedPlugin
import com.dtolabs.rundeck.core.plugins.configuration.Validator
import com.dtolabs.rundeck.plugins.notification.NotificationPlugin
import com.dtolabs.rundeck.core.plugins.DescribedPlugin
import grails.testing.web.controllers.ControllerUnitTest
import org.rundeck.app.authorization.AppAuthContextProcessor
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
            def service = 'AService'
            def name = 'someproperty'
            params.service = service
            params.name = name
            controller.pluginService = Mock(PluginService)
        when:
            def result = controller.pluginPropertiesValidateAjax( service, name)
        then:
            1 * controller.pluginService.validatePluginConfig(service, name, expected, null) >>
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
    void "validate ignored scope"() {
        given:
            request.content = json.bytes
            request.contentType = 'application/json'
            request.method = 'POST'
            request.addHeader('x-rundeck-ajax', 'true')
            def service = 'AService'
            def name = 'someproperty'
            params.service = service
            params.name = name
            params.ignoredScope=scopeText
            controller.pluginService = Mock(PluginService)
        when:
            def result = controller.pluginPropertiesValidateAjax( service, name)
        then:
            1 * controller.pluginService.validatePluginConfig(service, name, _,expectScope) >>
            new ValidatedPlugin(valid: true, report: Validator.buildReport().build())
            0 * controller.pluginService._(*_)
            response.status == 200
        where:
            json       | scopeText   | expectScope
            TEST_JSON1 | null        | null
            TEST_JSON1 | 'Instance'  | PropertyScope.Instance
            TEST_JSON1 | 'Project'   | PropertyScope.Project
            TEST_JSON1 | 'Framework' | PropertyScope.Framework
    }
    void "validate ignored scope invalid"() {
        given:
            request.content = json.bytes
            request.contentType = 'application/json'
            request.method = 'POST'
            request.addHeader('x-rundeck-ajax', 'true')
            def service = 'AService'
            def name = 'someproperty'
            params.service = service
            params.name = name
            params.ignoredScope=scopeText
            controller.pluginService = Mock(PluginService)
        when:
            def result = controller.pluginPropertiesValidateAjax( service, name)
        then:
            0 * controller.pluginService._(*_)
            response.status == 400
            response.json!=null
            response.json.error=='request.error.invalidrequest.message'

        where:
            json       | scopeText
            TEST_JSON1 | 'wrong'
            TEST_JSON1 | 'other'
    }
    void "pluginPropertiesValidateAjax missing plugin"() {
        given:
            request.content = json.bytes
            request.contentType = 'application/json'
            request.method = 'POST'
            request.addHeader('x-rundeck-ajax', 'true')
            def service = 'AService'
            def name = 'someproperty'
            params.service = service
            params.name = name
            controller.pluginService = Mock(PluginService)
        when:
            def result = controller.pluginPropertiesValidateAjax( service, name)
        then:
            1 * controller.pluginService.validatePluginConfig(service, name, expected, null) >> null
            0 * controller.pluginService._(*_)
            response.status == 404
            response.json != null
            response.json.valid == false
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

    def "plugin list filters by service"() {
        given:
            params.service = a
            controller.pluginService = Mock(PluginService)
            controller.pluginApiService = Mock(PluginApiService)
            controller.uiPluginService = Mock(UiPluginService)

            def plugins = [
                    [
                        service: 'WebhookEvent',
                        providers: [
                                [:],
                                [:]],
                    ],
                    [
                        service: 'Foo',
                        providers: [[:]]
                    ]
            ]

        when:
            def result = controller.listPlugins()
        then:
            1 * controller.pluginApiService.listPlugins() >> plugins
            response.json.size() == b

        where:
            a                | b
            null             | 3
            'WebhookEvent'   | 2
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
        controller.featureService = Mock(FeatureService)
        controller.frameworkService = Mock(FrameworkService)

        controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor)
        messageSource.addMessage("plugin.error.missing.upload.file",Locale.ENGLISH,"A plugin file must be specified")

        when:
        controller.uploadPlugin()

        then:
        1 * controller.rundeckAuthContextProcessor.getAuthContextForSubject(_)
        1 * controller.featureService.featurePresent(_) >> false
        1 * controller.rundeckAuthContextProcessor.authorizeApplicationResourceType(_,_,_) >> true
        response.text == '{"err":"A plugin file must be specified"}'
    }

    void "upload plugin, plugin security enabled"() {
        setup:
        controller.featureService = Mock(FeatureService)
        controller.frameworkService = Mock(FrameworkService)

        controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor)
        messageSource.addMessage("plugin.error.unauthorized.upload",Locale.ENGLISH,"Unable to upload plugins")

        when:
        controller.uploadPlugin()

        then:
        1 * controller.featureService.featurePresent(_) >> true
        response.text == '{"err":"Unable to upload plugins"}'
    }

    void "install plugin no plugin url specified"() {
        setup:
        controller.frameworkService = Mock(FrameworkService)

            controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor)
        messageSource.addMessage("plugin.error.missing.url",Locale.ENGLISH,"The plugin URL is required")

        when:
        controller.installPlugin()

        then:
        1 * controller.rundeckAuthContextProcessor.getAuthContextForSubject(_)
        1 * controller.rundeckAuthContextProcessor.authorizeApplicationResourceType(_,_,_) >> true
        response.text == '{"err":"The plugin URL is required"}'
    }

    void "upload plugin"() {
        setup:
        File uploaded = new File(uploadTestTargetDir,PLUGIN_FILE)
        def fwksvc = Mock(FrameworkService)

        controller.featureService = Mock(FeatureService)
            controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor)
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
        1 * controller.rundeckAuthContextProcessor.getAuthContextForSubject(_)
        1 * controller.featureService.featurePresent(_) >> false
        1 * controller.rundeckAuthContextProcessor.authorizeApplicationResourceType(_,_,_) >> true
        response.text == '{"msg":"done"}'
        uploaded.exists()

        cleanup:
        uploaded.delete()
    }

    void "install plugin"() {
        setup:
        File installed = new File(uploadTestTargetDir,PLUGIN_FILE)
        def fwksvc = Mock(FrameworkService)

            controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor)
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
        1 * controller.rundeckAuthContextProcessor.getAuthContextForSubject(_)
        1 * controller.rundeckAuthContextProcessor.authorizeApplicationResourceType(_,_,_) >> true
        response.text == '{"msg":"done"}'
        installed.exists()

        cleanup:
        installed.delete()
    }

    void "unauthorized install plugin fails"() {
        setup:
        controller.frameworkService = Mock(FrameworkService)

            controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor)
        messageSource.addMessage("request.error.unauthorized.title",Locale.ENGLISH,"Unauthorized")

        when:
        def pluginUrl = Thread.currentThread().getContextClassLoader().getResource(PLUGIN_FILE)
        params.pluginUrl = pluginUrl.toString()
        controller.installPlugin()

        then:
        1 * controller.rundeckAuthContextProcessor.getAuthContextForSubject(_)
        1 * controller.rundeckAuthContextProcessor.authorizeApplicationResourceType(_,_,_) >> false
        response.text == '{"err":"Unauthorized"}'
    }

    def "plugin detail for ui plugin"() {
        given:

            params.name = 'test1'
            params.service = 'UI'
            controller.pluginService = Mock(PluginService)
            controller.uiPluginService = Mock(UiPluginService)
            controller.pluginApiService = Mock(PluginApiService)
            def uiPluginProviderService = Mock(UIPluginProviderService)
            controller.frameworkService = Mock(FrameworkService)

            def plugin = Mock(UIPlugin)
            def description = DescriptionBuilder.builder().name('test1').build()
            def pluginMeta = Mock(PluginMetadata)
        when:
            controller.pluginDetail()
        then:
            response.status == 200

            response.contentType.startsWith 'application/json'

            1 * controller.uiPluginService.getUiPluginProviderService() >> uiPluginProviderService
            1 * controller.uiPluginService.getPluginMessage('UI', 'test1', 'plugin.title', _, _) >> 'ptitle'
            1 * controller.uiPluginService.getPluginMessage('UI', 'test1', 'plugin.description', _, _) >>
            'pdescription'
            1 * controller.pluginApiService.pluginPropertiesAsMap('UI', 'test1', _) >> []
            1 * controller.pluginService.getPluginDescriptor('test1', uiPluginProviderService) >>
            new DescribedPlugin<UIPlugin>(plugin, description, 'test1')
            1 * controller.frameworkService.getRundeckFramework() >> Mock(IFramework) {
                1 * getPluginManager() >> Mock(ServiceProviderLoader) {
                    1 * getPluginMetadata('UI', 'test1') >> pluginMeta
                }
            }
    }
}
