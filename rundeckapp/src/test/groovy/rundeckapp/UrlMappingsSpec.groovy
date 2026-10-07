package rundeckapp

import grails.testing.web.UrlMappingsUnitTest
import rundeck.controllers.ErrorController
import spock.lang.Specification

/**
 * Verifies that a direct request to /error resolves to a real error action instead of
 * falling into the generic controller/action mapping (which has no default action and fails).
 */
class UrlMappingsSpec extends Specification implements UrlMappingsUnitTest<UrlMappings> {

    void setup() {
        mockController(ErrorController)
    }

    void "direct GET /error maps to error notFound"() {
        expect:
        verifyForwardUrlMapping("/error", controller: 'error', action: 'notFound')
    }
}
