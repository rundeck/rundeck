package rundeck

import grails.testing.web.taglib.TagLibUnitTest
import rundeck.codecs.SanitizedHTMLCodec
import spock.lang.Specification
import spock.lang.Unroll

class PluginTagLibSpec extends Specification implements TagLibUnitTest<PluginTagLib> {

    def setup() {
        mockCodec(SanitizedHTMLCodec)
    }

    @Unroll
    def "customFields label falls back to the key when label is #desc"() {
        given:
            def json = groovy.json.JsonOutput.toJson([[key: 'env', label: label, value: 'prod']])
        when:
            def result = applyTemplate('<stepplugin:customFields json="${json}"/>', [json: json])
        then:
            result.contains('<span title="">env: </span>')
            result.contains('<span class="text-success">prod</span>')
        where:
            desc            | label
            'null'          | null
            'empty'         | ''
            'whitespace'    | '   '
    }

    def "customFields shows a non-blank label rather than the key"() {
        given:
            def json = groovy.json.JsonOutput.toJson([[key: 'env', label: 'Environment', value: 'prod']])
        when:
            def result = applyTemplate('<stepplugin:customFields json="${json}"/>', [json: json])
        then:
            result.contains('<span title="">Environment: </span>')
            !result.contains('env: ')
    }

    def "customFields renders an empty label when both label and key are missing"() {
        given:
            def json = groovy.json.JsonOutput.toJson([[label: ' ', value: 'prod']])
        when:
            def result = applyTemplate('<stepplugin:customFields json="${json}"/>', [json: json])
        then:
            result.contains('<span title="">: </span>')
    }
}
