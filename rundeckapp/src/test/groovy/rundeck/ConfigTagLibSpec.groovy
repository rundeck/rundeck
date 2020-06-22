package rundeck

import grails.testing.web.taglib.TagLibUnitTest
import rundeck.services.ConfigurationService
import spock.lang.Specification

class ConfigTagLibSpec extends Specification implements TagLibUnitTest<ConfigTagLib> {

    def setup() {
    }

    def cleanup() {
    }

    void "val"() {
        when:
        tagLib.configurationService = Mock(ConfigurationService) {
            getString(key,"") >> { return val }
        }
        def result = applyTemplate('<cfg:val key="'+key+'" />')

        then:
        result == val

        where:
        key | val
        "one" | "val-one"
        "two" | ""
    }
}
