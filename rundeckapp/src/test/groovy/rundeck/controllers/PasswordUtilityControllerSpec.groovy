package rundeck.controllers

import grails.testing.web.controllers.ControllerUnitTest
import spock.lang.Specification

class PasswordUtilityControllerSpec extends Specification implements ControllerUnitTest<PasswordUtilityController> {

    def setup() {
    }

    def cleanup() {
    }

    void "test encrypt jetty password"() {
        when:
        params.encrypter = "Jetty"
        params.valueToEncrypt = "somevalue"
        controller.encode()

        then:
        flash.output.obfuscate.startsWith("OBF")
        flash.output.md5.startsWith("MD5")
        flash.encrypter == "Jetty"
        response.redirectUrl == "/passwordUtility" +
        "/index"
    }
}
