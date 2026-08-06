package rundeck.services

import grails.testing.gorm.DataTest
import rundeck.ScheduledExecution
import rundeck.Workflow
import rundeck.CommandExec
import spock.lang.Specification

class JobSchedulesServiceSpec extends Specification implements DataTest {

    def setupSpec() {
        mockDomains(ScheduledExecution, Workflow, CommandExec)
    }

    def "bulkNextExecutionTime calls findAllByUuidInList with batches of at most 1000"() {
        given: "a LocalJobSchedulesManager with project execution/schedule checks stubbed"
        def service = new LocalJobSchedulesManager()
        def mockSES = Mock(ScheduledExecutionService) {
            isProjectScheduledEnabled(_) >> true
            isProjectExecutionEnabled(_) >> true
        }
        service.scheduledExecutionService = mockSES

        def batchSizes = []
        ScheduledExecution.metaClass.static.findAllByUuidInList = { List batch ->
            batchSizes << batch.size()
            return []
        }

        def uuids = (1..1001).collect { "uuid-${it}" }

        when:
        service.bulkNextExecutionTime("testProject", uuids)

        then:
        batchSizes.size() == 2
        batchSizes.every { it <= 1000 }
        batchSizes.sum() == 1001

        cleanup:
        ScheduledExecution.metaClass.static.findAllByUuidInList = null
    }
}
