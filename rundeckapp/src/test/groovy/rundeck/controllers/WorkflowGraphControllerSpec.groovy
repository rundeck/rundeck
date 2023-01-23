package rundeck.controllers

import grails.testing.web.controllers.ControllerUnitTest

import rundeck.services.ConfigurationService
import spock.lang.Specification

class WorkflowGraphControllerSpec extends Specification implements ControllerUnitTest<WorkflowGraphController> {

    def setup() {
    }

    def "render config property"() {
        given:
        controller.configurationService = Mock(ConfigurationService) {
            getBoolean("gui.workflowGraph", controller.defaultPropValue) >> controller.defaultPropValue
        }

        when:
        request.method = 'GET'
        def result = controller.WorkflowGraph()

        then:
        response.status == 200
        response.contentAsString == '{"showGraph":' + controller.defaultPropValue + '}'
    }
}