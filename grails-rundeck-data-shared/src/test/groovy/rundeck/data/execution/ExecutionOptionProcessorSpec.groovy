package rundeck.data.execution

import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import rundeck.data.job.RdJob
import rundeck.data.job.RdOption
import spock.lang.Specification
import spock.lang.Unroll

class ExecutionOptionProcessorSpec extends Specification {

    @Unroll
    def "parse job opts from string multivalue"() {
        given:
        ExecutionOptionProcessor processor = new ExecutionOptionProcessor()
        RdJob se = new RdJob()
        def opt1 = new RdOption(name: 'opt1', enforced: false, multivalued: true, delimiter: ',')
        final opt2 = new RdOption(name: 'opt2', enforced: true, multivalued: true, delimiter: ' ')
        opt2.delimiter = ' '
        opt2.valuesList = 'a,b,abc'
        se.optionSet = new TreeSet<>([opt1,opt2])


        when:
        def result = processor.parseJobOptsFromString(se, argString)

        then:
        result == expected

        where:
        argString                | expected
        '-opt1 test'             | [opt1: ['test']]
        '-opt1 test,x'           | [opt1: ['test', 'x']]
        '-opt1 \'test x\''       | [opt1: ['test x']]
        '-opt2 a'                | [opt2: ['a']]
        '-opt2 a,b'              | [opt2: ['a,b']]
        '-opt2 \'blah zah nah\'' | [opt2: ['blah', 'zah', 'nah']]
    }
}
