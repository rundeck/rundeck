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
        grailsApplication.config.clear()
        grailsApplication.config.rundeck.executionMode = 'active'
        then:
        service.executionModeActive
    }

    void "executionMode passive config"() {
        when:
        grailsApplication.config.clear()
        grailsApplication.config.rundeck.executionMode = 'passive'
        then:
        !service.executionModeActive
    }

    void "get string present"() {
        when:
        grailsApplication.config.clear()
        grailsApplication.config.rundeck.something.value = 'avalue'
        then:
        'avalue' == service.getString('something.value', 'blah')
    }

    void "get string missing"() {
        when:
        grailsApplication.config.clear()
        then:
        'blah' == service.getString('something.value', 'blah')
    }

    void "get boolean present"(testval,resultval) {
        when:
        grailsApplication.config.clear()
        grailsApplication.config.rundeck.something.value = testval
        then:
        resultval == service.getBoolean('something.value', false)

        where:
        testval | resultval
        'true'  | true
        true    | true
        'false' | false
        false   | false
    }

    void "get boolean missing"() {
        when:
        grailsApplication.config.clear()
        then:
        resultval == service.getBoolean('something.value', defval)

        where:
        defval | resultval
        true   | true
        false  | false
    }
}
