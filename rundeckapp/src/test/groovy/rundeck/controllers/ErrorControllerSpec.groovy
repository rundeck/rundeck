package rundeck.controllers

import grails.testing.web.controllers.ControllerUnitTest
import spock.lang.Specification
import spock.lang.Unroll

class ErrorControllerSpec extends Specification implements ControllerUnitTest<ErrorController> {

    @Unroll
    void "test 500 #format"() {
        when:
        response.format = format
        controller.fiveHundred()

        then:
        response.contentType == responseContentType
        response.text == expectedStartsWith

        where:
        format | responseContentType    | expectedStartsWith
        "html" | "text/html"            | ""
        "json" | "application/json"     | ErrorController.JSON_500
        "xml"  | "application/xml"      | ErrorController.XML_500
    }

    @Unroll
    void "test 405 #format"() {
        when:
        response.format = format
        controller.notAllowed()

        then:
        response.contentType == responseContentType
        response.text == expectedStartsWith

        where:
        format | responseContentType    | expectedStartsWith
        "html" | "text/html"            | ""
        "json" | "application/json"     | ErrorController.JSON_405
        "xml"  | "application/xml"      | ErrorController.XML_405
    }

    @Unroll
    void "test 404 #format"() {
        when:
        response.format = format
        controller.notFound()

        then:
        response.contentType == responseContentType
        response.text == expectedStartsWith

        where:
        format | responseContentType    | expectedStartsWith
        "html" | "text/html"            | ""
        "json" | "application/json"     | ErrorController.JSON_404
        "xml"  | "application/xml"      | ErrorController.XML_404
    }
}
