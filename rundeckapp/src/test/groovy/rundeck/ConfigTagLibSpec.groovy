package rundeck

import grails.testing.web.taglib.TagLibUnitTest
import rundeck.services.ConfigurationService
import spock.lang.Specification
import spock.lang.Unroll

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

    @Unroll
    def "get string"() {
        given:
            tagLib.configurationService = Mock(ConfigurationService) {
                getString("test.config.val", sdefVal) >> {
                    sconfVal != null ? sconfVal : it[1]
                }
            }
        when:
            def result = tagLib.getString(config: 'test.config.val', default: sdefVal)
        then:
            result == sexpected

        where:
            sconfVal | sdefVal | sexpected
            'abc'    | null    | 'abc'
            'xyz'    | 'asdf'  | 'xyz'
            null     | 'asdf'  | 'asdf'
    }

    @Unroll
    def "get boolean"() {
        given:
            tagLib.configurationService = Mock(ConfigurationService) {
                getBoolean("test.config.val", bdefVal) >> {
                    bconfVal != null ? bconfVal : it[1]
                }
            }
        when:
            def bresult = tagLib.getBoolean([config: 'test.config.val', default: bdefVal])
        then:
            bresult == bexpected

        where:
            bconfVal | bdefVal | bexpected
            true     | false   | true
            false    | true    | false
            null     | true    | true
            null     | false   | false
    }
    @Unroll
    def "get integer"() {
        given:
            tagLib.configurationService = Mock(ConfigurationService) {
                getInteger("test.config.val", idefVal) >> {
                    iconfVal != null ? iconfVal : it[1]
                }
            }
        when:
            def iresult = tagLib.getInteger([config: 'test.config.val', default: idefVal])
        then:
            iresult == iexpected

        where:
            iconfVal | idefVal | iexpected
            1        | 2       | 1
            null     | 2       | 2
    }

    def "get integer numberformat exception"() {
        given:
            tagLib.configurationService = Mock(ConfigurationService) {
                getInteger("test.config.val", idefVal) >> {
                    throw new NumberFormatException('expected exception')
                }
            }
        when:
            def iresult = tagLib.getInteger([config: 'test.config.val', default: idefVal])
        then:
            iresult == iexpected

        where:
            idefVal | iexpected
            2       | 2
    }
}
