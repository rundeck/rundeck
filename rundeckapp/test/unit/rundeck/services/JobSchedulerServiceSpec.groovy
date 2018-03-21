package rundeck.services

import com.dtolabs.rundeck.core.schedule.JobScheduleManager
import grails.test.mixin.TestFor
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
@TestFor(JobSchedulerService)
class JobSchedulerServiceSpec extends Specification {

    def setup() {
    }

    def cleanup() {
    }

    void "delete job delegates via bean"() {
        given:
        service.rundeckJobScheduleManager = Mock(JobScheduleManager)
        when:
        service.deleteJobSchedule('a', 'b')

        then:
        1 * service.rundeckJobScheduleManager.deleteJobSchedule('a', 'b')
    }

    void "schedule job now delegates via bean"() {
        given:
        service.rundeckJobScheduleManager = Mock(JobScheduleManager)
        when:
        service.scheduleJobNow('a', 'b', [:])

        then:
        1 * service.rundeckJobScheduleManager.scheduleJobNow('a', 'b', [:])
    }

    void "schedule job at date delegates via bean"() {
        given:
        service.rundeckJobScheduleManager = Mock(JobScheduleManager)
        Date date = new Date()
        when:
        service.scheduleJob('a', 'b', [:], date)

        then:
        1 * service.rundeckJobScheduleManager.scheduleJob('a', 'b', [:], date)
    }
}
