package rundeck.controllers

import com.dtolabs.rundeck.core.plugins.configuration.Validator
import com.dtolabs.rundeck.server.plugins.ValidatedPlugin
import grails.test.mixin.TestFor
import rundeck.services.PluginService
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.web.ControllerUnitTestMixin} for usage instructions
 */
@TestFor(PluginController)
class PluginControllerSpec extends Specification {

    def setup() {
    }

    def cleanup() {
    }
    static final String TEST_JSON1 = '''{"config":{"actions._indexes":"dbd3da9c_1","actions._type":"list","actions.entry[dbd3da9c_1].type":"testaction1","actions.entry[dbd3da9c_1].config.actions._type":"embedded","actions.entry[dbd3da9c_1].config.actions.type":"","actions.entry[dbd3da9c_1].config.actions.config.stringvalue":"asdf","actions.entry[dbd3da9c_1].config.actions":"{stringvalue=asdf}"},"report":{}}'''

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
        def result = controller.pluginPropertiesValidateAjax(project, service, name)


        then:
        1 * controller.pluginService.validatePluginConfig(service, name, expected, project) >>
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
}
