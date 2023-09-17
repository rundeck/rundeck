package rundeck.controllers

import grails.testing.web.controllers.ControllerUnitTest

import rundeck.services.ConfigurationService
import spock.lang.Specification

class HelplinkControllerSpec extends Specification implements ControllerUnitTest<HelplinkController> {

    def setup(){
    }

    def "render config property"() {
        given:
        controller.configurationService = Mock(ConfigurationService){
            getString("gui.helpLinkName", controller.defaultHelplinkName)>>controller.defaultHelplinkName
        }

        when:
        request.method='GET'
        def result=controller.helplinkName()

        then:
        response.status==200
        response.contentAsString == '{"name":"' + controller.defaultHelplinkName + '"}'
    }

}
