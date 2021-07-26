package rundeck.services

import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import org.rundeck.core.executions.provenance.Provenance
import org.rundeck.core.executions.provenance.ProvenanceComponent
import org.rundeck.core.executions.provenance.ProvenanceUtil
import rundeck.Execution
import rundeck.services.execution.BuiltinProvenanceComponent
import spock.lang.Specification
import spock.lang.Unroll

class ExecutionProvenanceServiceSpec extends Specification
    implements ServiceUnitTest<ExecutionProvenanceService>, DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        [Execution]
    }

    static class TestComponent implements ProvenanceComponent {
        @Override
        Map<String, Class<? extends Provenance<?>>> getProvenanceTypes() {
            [test: TestProvenance]
        }

        @Override
        def <T> boolean handlesProvenance(final Class<T> type) {
            return TestProvenance.isAssignableFrom(type)
        }

        @Override
        def <T> String describeProvenance(final T provenance) {
            return provenance.toString()
        }
    }

    static class TestProvenance implements Provenance<TestData> {
        TestData data

        @Override
        String toString() {
            return "TestProvenance: ${data.name}/${data.id}"
        }

        boolean equals(final o) {
            if (this.is(o)) {
                return true
            }
            if (getClass() != o.class) {
                return false
            }

            final TestProvenance that = (TestProvenance) o

            if (data != that.data) {
                return false
            }

            return true
        }

        int hashCode() {
            return data.hashCode()
        }
    }

    static class TestData {
        String name
        String id

        boolean equals(final o) {
            if (this.is(o)) {
                return true
            }
            if (getClass() != o.class) {
                return false
            }

            final TestData testData = (TestData) o

            if (id != testData.id) {
                return false
            }
            if (name != testData.name) {
                return false
            }

            return true
        }

        int hashCode() {
            int result
            result = name.hashCode()
            result = 31 * result + id.hashCode()
            return result
        }
    }

    def setup() {
        defineBeans {
            testProvenanceComponent(TestComponent)
        }
    }

    def cleanup() {
    }

    void "test component list"() {
        expect:
            service.getComponents() != null
            service.getComponents().size() == 1
            service.getComponents().get('testProvenanceComponent') instanceof TestComponent
    }

    @Unroll
    void "test setProvenanceForExecution test types"() {
        given:
            def e = new Execution()
            defineBeans{
                builtinProvenanceComponent(BuiltinProvenanceComponent)
            }
        when:
            service.setProvenanceForExecution(e, [provenance])
        then:
            e.provenanceData == json
        where:
            provenance | json
            new TestProvenance(data:new TestData(name:'x',id:'z'))|'{"provenances":[{"type":"test","data":{"name":"x","id":"z"}}]}'
    }
    @Unroll
    void "test getProvenanceForExecution test types"() {
        given:
            def e = new Execution()
            e.provenanceData=json
            defineBeans{
                builtinProvenanceComponent(BuiltinProvenanceComponent)
            }
        when:
            def provenances=service.getProvenanceForExecution(e)
        then:
            provenances==input
        where:
            input | json
            [new TestProvenance(data:new TestData(name:'x',id:'z'))]|'{"provenances":[{"type":"test","data":{"name":"x","id":"z"}}]}'
    }
    static Date schedule1 = new Date(1627341750923L)
    static Date schedule2 = new Date(schedule1.time+30000)
    @Unroll
    void "test setProvenanceForExecution builtin types"() {
        given:
            def e = new Execution()
            defineBeans{
                builtinProvenanceComponent(BuiltinProvenanceComponent)
            }
        when:
            service.setProvenanceForExecution(e, [provenance])
        then:
            e.provenanceData == json
        where:
            provenance | json
            ProvenanceUtil.generic(test: 'data')|'{"provenances":[{"type":"generic","data":{"test":"data"}}]}'
            ProvenanceUtil.scheduler('builtin','a','b','c')|'{"provenances":[{"type":"schedule","data":{"type":"builtin","name":"a","id":"b","crontabExpression":"c"}}]}'
            ProvenanceUtil.scheduledTrigger(schedule1,schedule2)|'{"provenances":[{"type":"schedule-trigger","data":{"scheduleTime":"2021-07-26T23:22:30.923+00:00","fireTime":"2021-07-26T23:23:00.923+00:00"}}]}'
            ProvenanceUtil.apiRequest('uri')|'{"provenances":[{"type":"api-request","data":{"requestUri":"uri"}}]}'
            ProvenanceUtil.webRequest('uri2')|'{"provenances":[{"type":"web-request","data":{"requestUri":"uri2"}}]}'
            ProvenanceUtil.executionFollowup('eid')|'{"provenances":[{"type":"execution","data":{"executionId":"eid"}}]}'
            ProvenanceUtil.plugin('a','b')|'{"provenances":[{"type":"plugin","data":{"provider":"a","service":"b"}}]}'
            ProvenanceUtil.stepPlugin('a','b','1/2')|'{"provenances":[{"type":"step-plugin","data":{"provider":"a","service":"b","stepCtx":"1/2"}}]}'
            ProvenanceUtil.retry('eid','reason')|'{"provenances":[{"type":"retry","data":{"executionId":"eid","reason":"reason"}}]}'
    }
}
