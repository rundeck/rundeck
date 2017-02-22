package rundeck

import grails.test.mixin.TestFor
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.domain.DomainClassUnitTestMixin} for usage instructions
 */
@TestFor(JobFileRecord)
class JobFileRecordSpec extends Specification {

    def setup() {
    }

    def cleanup() {
    }

    def "invalid state changes"() {
        given:
        def jfr = new JobFileRecord()

        when:
        jfr.fileState = JobFileRecord.STATE_RETAINED
        jfr.stateExpired()
        then:
        IllegalStateException e = thrown()

        when:
        jfr.fileState = JobFileRecord.STATE_DELETED
        jfr.stateExpired()
        then:
        e = thrown()

        when:
        jfr.fileState = JobFileRecord.STATE_DELETED
        jfr.stateRetained()
        then:
        e = thrown()

        when:
        jfr.fileState = JobFileRecord.STATE_EXPIRED
        jfr.stateRetained()
        then:
        e = thrown()
        when:
        jfr.fileState = JobFileRecord.STATE_EXPIRED
        jfr.stateDeleted()
        then:
        e = thrown()

    }

    void "valid state changes"() {
        given:
        def jfr = new JobFileRecord()

        when:
        jfr.fileState = JobFileRecord.STATE_TEMP
        jfr.stateRetained()
        then:
        jfr.stateIsAvailable()

        when:
        jfr.fileState = JobFileRecord.STATE_TEMP
        jfr.stateExpired()
        then:
        jfr.stateIsExpired()

        when:
        jfr.fileState = JobFileRecord.STATE_RETAINED
        jfr.stateDeleted()
        then:
        jfr.stateIsDeleted()

        when:
        jfr.fileState = JobFileRecord.STATE_TEMP
        jfr.stateDeleted()
        then:
        jfr.stateIsDeleted()
    }
}
