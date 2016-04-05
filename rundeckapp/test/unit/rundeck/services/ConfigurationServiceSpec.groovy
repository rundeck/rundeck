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

    void "get integer"() {
        when:
        grailsApplication.config.clear()
        grailsApplication.config.rundeck.something.value = confVal
        then:
        expval == service.getInteger('something.value', defval)

        where:
        confVal | expval | defval
        null    | 1      | 1
        12      | 12     | 1
        12L     | 12     | 1
        '12'    | 12     | 1
        '3'     | 3      | 1
    }

    void "get long"() {
        when:
        grailsApplication.config.clear()
        grailsApplication.config.rundeck.something.value = confVal
        then:
        expval == service.getLong('something.value', defval)

        where:
        confVal | expval | defval
        null    | 1L     | 1L
        12      | 12L    | 1L
        12L     | 12L    | 1L
        '12'    | 12L    | 1L
        '3'     | 3L     | 1L
    }


    void "get boolean present"(testval, resultval) {
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
