package rundeck.services

import grails.test.mixin.TestFor
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
@TestFor(ConfigurationService)
class ConfigurationServiceSpec extends Specification {

    def setup() {
    }

    def cleanup() {
    }

    void "executionMode active config"() {
        when:
        grailsApplication.config.rundeck.executionMode='active'
        then:
        service.executionModeActive
    }
    void "executionMode passive config"() {
        when:
        grailsApplication.config.rundeck.executionMode='passive'
        then:
        !service.executionModeActive
    }
}
