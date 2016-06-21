package rundeck

import grails.test.mixin.TestFor
import spock.lang.Specification

/**
 * Created by greg on 6/21/16.
 */
@TestFor(UtilityTagLib)
class UtilityTagLibSpec extends Specification {
    def "text after"() {
        given:
        def text = '''abc
123
456
---
789
'''
        when:
        def result = tagLib.textAfterLine(text: text, marker: '---').toString()

        then:
        result == '789\n'

    }
    def "text before"() {
        given:
        def text = '''abc
123
456
---
789
'''
        when:
        def result = tagLib.textBeforeLine(text: text, marker: '---').toString()

        then:
        result == '''abc
123
456'''

    }
}
